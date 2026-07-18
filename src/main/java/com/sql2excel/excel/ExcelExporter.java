package com.sql2excel.excel;

import com.sql2excel.config.ExcelConfig;
import com.sql2excel.config.SheetConfig;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class ExcelExporter {

    public void export(String outputPath, List<SheetData> sheets, ExcelConfig config) throws IOException {
        if (sheets == null || sheets.isEmpty()) {
            throw new IllegalArgumentException("No sheet data to export");
        }

        XSSFWorkbook workbook = new XSSFWorkbook();

        CellStyle headerStyle = createStyle(workbook, config.getHeader());
        CellStyle bodyStyle = createStyle(workbook, config.getBody());
        DataFormat dataFormat = workbook.createDataFormat();

        Set<String> usedSheetNames = new HashSet<>();
        for (SheetData data : sheets) {
            addSheet(workbook, data, headerStyle, bodyStyle, usedSheetNames);
        }

        Path outPath = Paths.get(outputPath);
        Files.createDirectories(outPath.getParent());
        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            workbook.write(fos);
        } finally {
            workbook.close();
        }
    }

    private void addSheet(XSSFWorkbook workbook, SheetData data, CellStyle headerStyle, CellStyle bodyStyle, Set<String> usedNames) {
        String sheetName = makeUniqueSheetName(data.getName(), usedNames);
        Sheet sheet = workbook.createSheet(sheetName);

        List<String> columns = data.getColumns();
        List<Map<String, Object>> rows = data.getRows();

        // Header
        Row header = sheet.createRow(0);
        for (int i = 0; i < columns.size(); i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns.get(i));
            if (headerStyle != null) {
                cell.setCellStyle(headerStyle);
            }
        }

        // Body
        for (int r = 0; r < rows.size(); r++) {
            Row row = sheet.createRow(r + 1);
            Map<String, Object> record = rows.get(r);
            for (int i = 0; i < columns.size(); i++) {
                Cell cell = row.createCell(i);
                setCellValue(cell, record.get(columns.get(i)));
                if (bodyStyle != null) {
                    cell.setCellStyle(bodyStyle);
                }
            }
        }

        // Auto size columns
        for (int i = 0; i < columns.size(); i++) {
            try {
                sheet.autoSizeColumn(i);
                int width = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, Math.min(Math.max(width, 256 * 10), 256 * 50));
            } catch (Exception ignored) {
            }
        }

        // Freeze header row
        sheet.createFreezePane(0, 1);
    }

    private CellStyle createStyle(XSSFWorkbook workbook, Map<String, Object> styleMap) {
        if (styleMap == null || styleMap.isEmpty()) {
            return null;
        }
        XSSFCellStyle style = workbook.createCellStyle();

        // Font
        Object fontObj = styleMap.get("font");
        if (fontObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> fontMap = (Map<String, Object>) fontObj;
            XSSFFont font = workbook.createFont();
            Object name = fontMap.get("name");
            if (name != null) {
                font.setFontName(name.toString());
            }
            Object size = fontMap.get("size");
            if (size instanceof Number) {
                font.setFontHeightInPoints(((Number) size).shortValue());
            } else if (size != null) {
                try {
                    font.setFontHeightInPoints(Short.parseShort(size.toString()));
                } catch (NumberFormatException ignored) {
                }
            }
            Object bold = fontMap.get("bold");
            if (Boolean.TRUE.equals(bold) || "true".equalsIgnoreCase(String.valueOf(bold))) {
                font.setBold(true);
            }
            Object color = fontMap.get("color");
            if (color != null) {
                byte[] rgb = parseColor(color.toString());
                if (rgb != null) {
                    font.setColor(new XSSFColor(rgb, null));
                }
            }
            style.setFont(font);
        }

        // Fill
        Object fillObj = styleMap.get("fill");
        if (fillObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> fillMap = (Map<String, Object>) fillObj;
            Object color = fillMap.get("color");
            if (color != null) {
                byte[] rgb = parseColor(color.toString());
                if (rgb != null) {
                    style.setFillForegroundColor(new XSSFColor(rgb, null));
                    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                }
            }
        }

        // Alignment
        Object alignObj = styleMap.get("alignment");
        if (alignObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> alignMap = (Map<String, Object>) alignObj;
            Object h = alignMap.get("horizontal");
            if (h != null) {
                style.setAlignment(parseHorizontalAlignment(h.toString()));
            }
            Object v = alignMap.get("vertical");
            if (v != null) {
                style.setVerticalAlignment(parseVerticalAlignment(v.toString()));
            }
        }

        // Border
        Object borderObj = styleMap.get("border");
        if (borderObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> borderMap = (Map<String, Object>) borderObj;
            Object allObj = borderMap.get("all");
            if (allObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> all = (Map<String, Object>) allObj;
                BorderStyle bs = parseBorderStyle(all.get("style"));
                if (bs != null) {
                    style.setBorderTop(bs);
                    style.setBorderBottom(bs);
                    style.setBorderLeft(bs);
                    style.setBorderRight(bs);
                }
                Object color = all.get("color");
                if (color != null) {
                    byte[] rgb = parseColor(color.toString());
                    if (rgb != null) {
                        XSSFColor c = new XSSFColor(rgb, null);
                        style.setTopBorderColor(c);
                        style.setBottomBorderColor(c);
                        style.setLeftBorderColor(c);
                        style.setRightBorderColor(c);
                    }
                }
            }
        }

        return style;
    }

    private void setCellValue(Cell cell, Object value) {
        if (value == null) {
            cell.setBlank();
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof java.sql.Timestamp || value instanceof java.util.Date) {
            cell.setCellValue((java.util.Date) value);
        } else if (value instanceof LocalDateTime) {
            cell.setCellValue((LocalDateTime) value);
        } else if (value instanceof LocalDate) {
            cell.setCellValue((LocalDate) value);
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else {
            cell.setCellValue(value.toString());
        }
    }

    private byte[] parseColor(String color) {
        String hex = color.startsWith("#") ? color.substring(1) : color;
        if (hex.length() == 6) {
            return new byte[]{
                    (byte) Integer.parseInt(hex.substring(0, 2), 16),
                    (byte) Integer.parseInt(hex.substring(2, 4), 16),
                    (byte) Integer.parseInt(hex.substring(4, 6), 16)
            };
        }
        return null;
    }

    private HorizontalAlignment parseHorizontalAlignment(String value) {
        switch (value.toLowerCase()) {
            case "center":
                return HorizontalAlignment.CENTER;
            case "right":
                return HorizontalAlignment.RIGHT;
            case "left":
            default:
                return HorizontalAlignment.LEFT;
        }
    }

    private VerticalAlignment parseVerticalAlignment(String value) {
        switch (value.toLowerCase()) {
            case "top":
                return VerticalAlignment.TOP;
            case "bottom":
                return VerticalAlignment.BOTTOM;
            case "middle":
            default:
                return VerticalAlignment.CENTER;
        }
    }

    private BorderStyle parseBorderStyle(Object value) {
        if (value == null) {
            return null;
        }
        switch (value.toString().toLowerCase()) {
            case "thin":
                return BorderStyle.THIN;
            case "medium":
                return BorderStyle.MEDIUM;
            case "thick":
                return BorderStyle.THICK;
            case "dashed":
                return BorderStyle.DASHED;
            case "dotted":
                return BorderStyle.DOTTED;
            case "double":
                return BorderStyle.DOUBLE;
            case "hair":
                return BorderStyle.HAIR;
            default:
                return BorderStyle.NONE;
        }
    }

    private String makeUniqueSheetName(String name, Set<String> usedNames) {
        String base = safeSheetName(name);
        String candidate = base;
        int index = 1;
        while (usedNames.contains(candidate)) {
            String suffix = " (" + index + ")";
            int maxBaseLen = 31 - suffix.length();
            candidate = base.substring(0, Math.min(base.length(), maxBaseLen)) + suffix;
            index++;
        }
        usedNames.add(candidate);
        return candidate;
    }

    private String safeSheetName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "Sheet";
        }
        String trimmed = name.trim();
        String sanitized = trimmed.replaceAll("[*\\/\\?:\\[\\]]", "_");
        if (sanitized.length() > 31) {
            sanitized = sanitized.substring(0, 31);
        }
        if (sanitized.startsWith("'") || sanitized.endsWith("'")) {
            sanitized = sanitized.replace("'", "_");
        }
        if (sanitized.isEmpty()) {
            sanitized = "Sheet";
        }
        return sanitized;
    }

    public static List<String> filterColumns(List<String> columns, String exceptColumns) {
        if (exceptColumns == null || exceptColumns.isEmpty()) {
            return columns;
        }
        Set<String> excludes = new HashSet<>();
        for (String s : exceptColumns.split(",")) {
            excludes.add(s.trim());
        }
        List<String> filtered = new ArrayList<>();
        for (String col : columns) {
            if (!excludes.contains(col)) {
                filtered.add(col);
            }
        }
        return filtered;
    }

    public static class SheetData {
        private final String name;
        private final List<String> columns;
        private final List<Map<String, Object>> rows;

        public SheetData(String name, List<String> columns, List<Map<String, Object>> rows) {
            this.name = name;
            this.columns = columns;
            this.rows = rows;
        }

        public String getName() {
            return name;
        }

        public List<String> getColumns() {
            return columns;
        }

        public List<Map<String, Object>> getRows() {
            return rows;
        }
    }
}
