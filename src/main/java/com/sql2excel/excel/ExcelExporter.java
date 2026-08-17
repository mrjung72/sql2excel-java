package com.sql2excel.excel;

import com.sql2excel.config.ExcelConfig;
import com.sql2excel.config.SheetConfig;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.DateFormatConverter;
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

    public void export(String outputPath, List<SheetData> sheets) throws IOException {
        if (sheets == null || sheets.isEmpty()) {
            throw new IllegalArgumentException("No sheet data to export");
        }

        XSSFWorkbook workbook = new XSSFWorkbook();

        Set<String> usedSheetNames = new HashSet<>();
        List<String> dataSheetNames = new ArrayList<>();
        boolean tocAdded = false;
        for (SheetData data : sheets) {
            String actualName = addSheet(workbook, data, usedSheetNames);
            if ("목차".equals(data.getName()) && !tocAdded) {
                tocAdded = true;
            } else {
                dataSheetNames.add(actualName);
            }
        }
        addTocHyperlinks(workbook, dataSheetNames);

        Path outPath = Paths.get(outputPath);
        Files.createDirectories(outPath.getParent());
        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            workbook.write(fos);
        } finally {
            workbook.close();
        }
    }

    private String addSheet(XSSFWorkbook workbook, SheetData data, Set<String> usedNames) {
        CellStyle headerStyle = createStyle(workbook, data.getHeader());
        Map<String, CellStyle> bodyStyles = createBodyStyles(workbook, data.getBody());
        CellStyle defaultBodyStyle = bodyStyles.get("default");
        CellStyle numberBodyStyle = bodyStyles.getOrDefault("number", defaultBodyStyle);
        CellStyle dateBodyStyle = bodyStyles.getOrDefault("date", defaultBodyStyle);
        String sheetName = makeUniqueSheetName(data.getName(), usedNames);
        Sheet sheet = workbook.createSheet(sheetName);

        List<String> columns = data.getColumns();
        List<Map<String, Object>> rows = data.getRows();

        // Date column format
        CellStyle dateStyle = null;
        String dateColumnFormat = data.getDateColumnFormat();
        if (dateColumnFormat != null && !dateColumnFormat.isEmpty()) {
            try {
                String excelFormat = DateFormatConverter.convert(Locale.getDefault(), dateColumnFormat);
                short fmt = workbook.createDataFormat().getFormat(excelFormat);
                dateStyle = workbook.createCellStyle();
                if (dateBodyStyle != null) {
                    dateStyle.cloneStyleFrom(dateBodyStyle);
                }
                dateStyle.setDataFormat(fmt);
            } catch (Exception ignored) {
            }
        }

        int startRow = 0;
        String sheetComments = data.getSheetComments();
        if (sheetComments != null && !sheetComments.isEmpty()) {
            Row topRow = sheet.createRow(0);
            Cell commentCell = topRow.createCell(0);
            commentCell.setCellValue(sheetComments);
            if (columns.size() > 1) {
                sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, columns.size() - 1));
            }
            startRow = 1;
        }

        // Column comments
        if (data.isApplyColumnComment()) {
            Row commentRow = sheet.createRow(startRow);
            Map<String, String> columnComments = data.getColumnComments();
            for (int i = 0; i < columns.size(); i++) {
                Cell cell = commentRow.createCell(i);
                String comment = columnComments != null ? columnComments.get(columns.get(i)) : null;
                cell.setCellValue(comment != null ? comment : "");
                if (headerStyle != null) {
                    cell.setCellStyle(headerStyle);
                }
            }
            startRow++;
        }

        // Header
        Row header = sheet.createRow(startRow);
        for (int i = 0; i < columns.size(); i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns.get(i));
            if (headerStyle != null) {
                cell.setCellStyle(headerStyle);
            }
        }

        // Body
        for (int r = 0; r < rows.size(); r++) {
            Row row = sheet.createRow(startRow + r + 1);
            Map<String, Object> record = rows.get(r);
            for (int i = 0; i < columns.size(); i++) {
                Cell cell = row.createCell(i);
                Object value = record.get(columns.get(i));
                setCellValue(cell, value);
                if (isDateValue(value)) {
                    if (dateStyle != null) {
                        cell.setCellStyle(dateStyle);
                    } else if (dateBodyStyle != null) {
                        cell.setCellStyle(dateBodyStyle);
                    }
                } else if (value instanceof Number && numberBodyStyle != null) {
                    cell.setCellStyle(numberBodyStyle);
                } else if (defaultBodyStyle != null) {
                    cell.setCellStyle(defaultBodyStyle);
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

        // Hide columns
        for (String col : data.getHiddenColumns()) {
            int index = columns.indexOf(col);
            if (index >= 0) {
                sheet.setColumnHidden(index, true);
            }
        }

        // Freeze header row
        sheet.createFreezePane(0, startRow + 1);
        return sheetName;
    }

    private void addTocHyperlinks(XSSFWorkbook workbook, List<String> dataSheetNames) {
        Sheet tocSheet = workbook.getSheet("목차");
        if (tocSheet == null) {
            return;
        }
        Row headerRow = tocSheet.getRow(0);
        if (headerRow == null) {
            return;
        }
        int sheetNameCol = -1;
        for (Cell cell : headerRow) {
            if ("시트명".equals(cell.getStringCellValue())) {
                sheetNameCol = cell.getColumnIndex();
                break;
            }
        }
        if (sheetNameCol < 0) {
            return;
        }

        CreationHelper helper = workbook.getCreationHelper();
        for (int i = 1; i <= tocSheet.getLastRowNum(); i++) {
            Row row = tocSheet.getRow(i);
            if (row == null) {
                continue;
            }
            Cell cell = row.getCell(sheetNameCol, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (cell == null) {
                continue;
            }
            if (i - 1 >= dataSheetNames.size()) {
                break;
            }
            String targetSheet = dataSheetNames.get(i - 1);
            if (workbook.getSheet(targetSheet) == null) {
                continue;
            }

            Hyperlink link = helper.createHyperlink(HyperlinkType.DOCUMENT);
            link.setAddress("'" + targetSheet + "'!A1");
            cell.setHyperlink(link);
            cell.setCellStyle(createHyperlinkStyle(workbook, cell.getCellStyle()));
        }

        // Limit TOC row height to a maximum of 50 points
        for (int i = 0; i <= tocSheet.getLastRowNum(); i++) {
            Row row = tocSheet.getRow(i);
            if (row == null) {
                continue;
            }
            row.setHeightInPoints(50f);
        }
    }

    private CellStyle createHyperlinkStyle(XSSFWorkbook workbook, CellStyle baseStyle) {
        XSSFCellStyle style = (XSSFCellStyle) workbook.createCellStyle();
        if (baseStyle != null) {
            style.cloneStyleFrom(baseStyle);
        }
        Font linkFont = workbook.createFont();
        if (baseStyle != null) {
            Font baseFont = workbook.getFontAt(baseStyle.getFontIndex());
            if (baseFont != null) {
                linkFont.setFontName(baseFont.getFontName());
                linkFont.setFontHeightInPoints(baseFont.getFontHeightInPoints());
                linkFont.setBold(baseFont.getBold());
                linkFont.setItalic(baseFont.getItalic());
            }
        }
        linkFont.setUnderline(FontUnderline.SINGLE.getByteValue());
        linkFont.setColor(IndexedColors.BLUE.getIndex());
        style.setFont(linkFont);
        return style;
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

        // Wrap text
        Object wrap = styleMap.get("wrap");
        if (Boolean.TRUE.equals(wrap) || "true".equalsIgnoreCase(String.valueOf(wrap))) {
            style.setWrapText(true);
        }

        return style;
    }

    private Map<String, CellStyle> createBodyStyles(XSSFWorkbook workbook, Map<String, Object> body) {
        Map<String, CellStyle> result = new HashMap<>();
        if (body == null || body.isEmpty()) {
            return result;
        }
        boolean nested = body.containsKey("default") && body.get("default") instanceof Map;
        @SuppressWarnings("unchecked")
        Map<String, Object> defaultMap = nested ? (Map<String, Object>) body.get("default") : body;
        CellStyle defaultStyle = createStyle(workbook, defaultMap);
        result.put("default", defaultStyle);
        result.put("number", createDerivedBodyStyle(workbook, body, "number", nested, defaultMap, defaultStyle));
        result.put("date", createDerivedBodyStyle(workbook, body, "date", nested, defaultMap, defaultStyle));
        return result;
    }

    @SuppressWarnings("unchecked")
    private CellStyle createDerivedBodyStyle(XSSFWorkbook workbook, Map<String, Object> body, String key,
                                             boolean nested, Map<String, Object> defaultMap, CellStyle defaultStyle) {
        if (!nested) {
            return defaultStyle;
        }
        Map<String, Object> subMap = body.containsKey(key) && body.get(key) instanceof Map
                ? (Map<String, Object>) body.get(key)
                : defaultMap;
        if (subMap == defaultMap) {
            return defaultStyle;
        }
        CellStyle derived = createStyle(workbook, subMap);
        if ("number".equals(key)) {
            applyNumberFormat(workbook, derived, subMap);
        }
        return derived;
    }

    @SuppressWarnings("unchecked")
    private void applyNumberFormat(XSSFWorkbook workbook, CellStyle style, Map<String, Object> styleMap) {
        if (style == null || styleMap == null) {
            return;
        }
        Object numberFormat = styleMap.get("numberFormat");
        if (!(numberFormat instanceof Map)) {
            return;
        }
        Map<String, Object> fmtMap = (Map<String, Object>) numberFormat;
        int decimal = parseIntOrDefault(fmtMap.get("decimal"), 0);
        boolean thousands = parseBooleanOrDefault(fmtMap.get("thousands"), false);
        String pattern = buildNumberFormatPattern(decimal, thousands);
        if (pattern != null) {
            style.setDataFormat(workbook.createDataFormat().getFormat(pattern));
        }
    }

    private String buildNumberFormatPattern(int decimal, boolean thousands) {
        if (decimal < 0) {
            decimal = 0;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(thousands ? "#,##0" : "0");
        if (decimal > 0) {
            sb.append(".");
            for (int i = 0; i < decimal; i++) {
                sb.append("0");
            }
        }
        return sb.toString();
    }

    private int parseIntOrDefault(Object obj, int defaultValue) {
        if (obj == null) {
            return defaultValue;
        }
        try {
            if (obj instanceof Number) {
                return ((Number) obj).intValue();
            }
            return Integer.parseInt(obj.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private boolean parseBooleanOrDefault(Object obj, boolean defaultValue) {
        if (obj == null) {
            return defaultValue;
        }
        return Boolean.TRUE.equals(obj) || "true".equalsIgnoreCase(String.valueOf(obj));
    }

    private void setCellValue(Cell cell, Object value) {
        if (value == null) {
            cell.setBlank();
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof java.util.Date) {
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

    private boolean isDateValue(Object value) {
        return value instanceof java.util.Date || value instanceof LocalDateTime || value instanceof LocalDate;
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
        private final Map<String, Object> header;
        private final Map<String, Object> body;
        private final List<String> hiddenColumns;
        private final String dateColumnFormat;
        private final String locDesc;
        private final String sheetComments;
        private final boolean applyColumnComment;
        private final Map<String, String> columnComments;

        public SheetData(String name, List<String> columns, List<Map<String, Object>> rows,
                         Map<String, Object> header, Map<String, Object> body) {
            this(name, columns, rows, header, body, Collections.emptyList(), null, null, null, false, null);
        }

        public SheetData(String name, List<String> columns, List<Map<String, Object>> rows,
                         Map<String, Object> header, Map<String, Object> body,
                         List<String> hiddenColumns) {
            this(name, columns, rows, header, body, hiddenColumns, null, null, null, false, null);
        }

        public SheetData(String name, List<String> columns, List<Map<String, Object>> rows,
                         Map<String, Object> header, Map<String, Object> body,
                         List<String> hiddenColumns, String dateColumnFormat) {
            this(name, columns, rows, header, body, hiddenColumns, dateColumnFormat, null, null, false, null);
        }

        public SheetData(String name, List<String> columns, List<Map<String, Object>> rows,
                         Map<String, Object> header, Map<String, Object> body,
                         List<String> hiddenColumns, String dateColumnFormat,
                         String locDesc, String sheetComments) {
            this(name, columns, rows, header, body, hiddenColumns, dateColumnFormat, locDesc, sheetComments, false, null);
        }

        public SheetData(String name, List<String> columns, List<Map<String, Object>> rows,
                         Map<String, Object> header, Map<String, Object> body,
                         List<String> hiddenColumns, String dateColumnFormat,
                         String locDesc, String sheetComments,
                         boolean applyColumnComment, Map<String, String> columnComments) {
            this.name = name;
            this.columns = columns;
            this.rows = rows;
            this.header = header;
            this.body = body;
            this.hiddenColumns = hiddenColumns;
            this.dateColumnFormat = dateColumnFormat;
            this.locDesc = locDesc;
            this.sheetComments = sheetComments;
            this.applyColumnComment = applyColumnComment;
            this.columnComments = columnComments;
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

        public Map<String, Object> getHeader() {
            return header;
        }

        public Map<String, Object> getBody() {
            return body;
        }

        public List<String> getHiddenColumns() {
            return hiddenColumns;
        }

        public String getDateColumnFormat() {
            return dateColumnFormat;
        }

        public String getLocDesc() {
            return locDesc;
        }

        public String getSheetComments() {
            return sheetComments;
        }

        public boolean isApplyColumnComment() {
            return applyColumnComment;
        }

        public Map<String, String> getColumnComments() {
            return columnComments;
        }
    }
}
