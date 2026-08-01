package com.sql2excel.xml;

import com.sql2excel.excel.ExcelExporter;
import com.sql2excel.export.ValueFormatter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class XmlExporter {

    public List<String> export(String outputPath, List<ExcelExporter.SheetData> sheets) throws IOException {
        if (sheets == null || sheets.isEmpty()) {
            throw new IllegalArgumentException("No sheet data to export");
        }

        Path outPath = Paths.get(outputPath);
        Files.createDirectories(outPath.getParent());

        writeAttributeXml(outPath, sheets);

        String elementPath = deriveElementPath(outputPath);
        writeElementXml(Paths.get(elementPath), sheets);

        return List.of(outputPath, elementPath);
    }

    private void writeAttributeXml(Path outputPath, List<ExcelExporter.SheetData> sheets) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(Files.newOutputStream(outputPath), "UTF-8"))) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            writer.newLine();
            writer.write("<sheets>");
            writer.newLine();

            for (ExcelExporter.SheetData sheet : sheets) {
                writer.write("  <sheet name=\"" + escapeXml(sheet.getName()) + "\">");
                writer.newLine();

                List<String> visibleColumns = visibleColumnsOf(sheet);

                for (Map<String, Object> row : sheet.getRows()) {
                    writer.write("    <row>");
                    writer.newLine();
                    for (String col : visibleColumns) {
                        Object value = row.get(col);
                        String type = determineType(value);
                        Object formatted = ValueFormatter.formatValue(value, sheet.getDateColumnFormat());
                        writer.write("      <col name=\"" + escapeXml(col) + "\" type=\"" + escapeXml(type) + "\">");
                        if (formatted != null) {
                            writer.write(escapeXml(String.valueOf(formatted)));
                        }
                        writer.write("</col>");
                        writer.newLine();
                    }
                    writer.write("    </row>");
                    writer.newLine();
                }

                writer.write("  </sheet>");
                writer.newLine();
            }

            writer.write("</sheets>");
            writer.newLine();
        }
    }

    private void writeElementXml(Path outputPath, List<ExcelExporter.SheetData> sheets) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(Files.newOutputStream(outputPath), "UTF-8"))) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            writer.newLine();
            writer.write("<sheets>");
            writer.newLine();

            for (ExcelExporter.SheetData sheet : sheets) {
                writer.write("  <sheet name=\"" + escapeXml(sheet.getName()) + "\">");
                writer.newLine();

                List<String> visibleColumns = visibleColumnsOf(sheet);
                List<String> elementNames = toElementNames(visibleColumns);

                for (Map<String, Object> row : sheet.getRows()) {
                    writer.write("    <row>");
                    writer.newLine();
                    for (int i = 0; i < visibleColumns.size(); i++) {
                        String col = visibleColumns.get(i);
                        String elName = elementNames.get(i);
                        Object value = row.get(col);
                        Object formatted = ValueFormatter.formatValue(value, sheet.getDateColumnFormat());
                        writer.write("      <" + elName + ">");
                        if (formatted != null) {
                            writer.write(escapeXml(String.valueOf(formatted)));
                        }
                        writer.write("</" + elName + ">");
                        writer.newLine();
                    }
                    writer.write("    </row>");
                    writer.newLine();
                }

                writer.write("  </sheet>");
                writer.newLine();
            }

            writer.write("</sheets>");
            writer.newLine();
        }
    }

    private List<String> visibleColumnsOf(ExcelExporter.SheetData sheet) {
        List<String> visibleColumns = new ArrayList<>(sheet.getColumns());
        List<String> hidden = sheet.getHiddenColumns();
        if (hidden != null && !hidden.isEmpty()) {
            visibleColumns.removeAll(hidden);
        }
        return visibleColumns;
    }

    private String deriveElementPath(String outputPath) {
        Path p = Paths.get(outputPath);
        String fileName = p.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String base = dot > 0 ? fileName.substring(0, dot) : fileName;
        String ext = dot > 0 ? fileName.substring(dot) : ".xml";
        if (base.endsWith("_element")) {
            return p.resolveSibling(base + ext).toString();
        }
        return p.resolveSibling(base + "_element" + ext).toString();
    }

    private List<String> toElementNames(List<String> columns) {
        List<String> names = new ArrayList<>();
        Set<String> used = new HashSet<>();
        for (String col : columns) {
            String name = toElementName(col);
            if (!used.add(name)) {
                int i = 2;
                while (!used.add(name + "_" + i)) {
                    i++;
                }
                name = name + "_" + i;
            }
            names.add(name);
        }
        return names;
    }

    private String toElementName(String name) {
        if (name == null || name.isEmpty()) {
            return "col";
        }
        String sanitized = name.replaceAll("[^\\p{L}\\p{N}_.\\-]", "_");
        if (sanitized.isEmpty()) {
            return "col";
        }
        char first = sanitized.charAt(0);
        if (!Character.isLetter(first) && first != '_') {
            sanitized = "_" + sanitized;
        }
        if (sanitized.startsWith("xml") || sanitized.startsWith("XML")) {
            sanitized = "_" + sanitized;
        }
        return sanitized;
    }

    private String determineType(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number) {
            return "number";
        }
        if (value instanceof Boolean) {
            return "boolean";
        }
        if (ValueFormatter.isDateValue(value)) {
            return "date";
        }
        return "string";
    }

    private String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '&':
                    sb.append("&amp;");
                    break;
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                case '\'':
                    sb.append("&apos;");
                    break;
                default:
                    if (c >= 0x20 || c == '\t' || c == '\n' || c == '\r') {
                        sb.append(c);
                    } else {
                        sb.append('?');
                    }
                    break;
            }
        }
        return sb.toString();
    }
}
