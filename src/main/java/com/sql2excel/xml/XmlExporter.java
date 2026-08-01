package com.sql2excel.xml;

import com.sql2excel.excel.ExcelExporter;
import com.sql2excel.export.ValueFormatter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class XmlExporter {

    public List<String> export(String outputPath, List<ExcelExporter.SheetData> sheets) throws IOException {
        if (sheets == null || sheets.isEmpty()) {
            throw new IllegalArgumentException("No sheet data to export");
        }

        Files.createDirectories(Paths.get(outputPath).getParent());

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(Files.newOutputStream(Paths.get(outputPath)), "UTF-8"))) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            writer.newLine();
            writer.write("<sheets>");
            writer.newLine();

            for (ExcelExporter.SheetData sheet : sheets) {
                writer.write("  <sheet name=\"" + escapeXml(sheet.getName()) + "\">");
                writer.newLine();

                List<String> visibleColumns = new ArrayList<>(sheet.getColumns());
                List<String> hidden = sheet.getHiddenColumns();
                if (hidden != null && !hidden.isEmpty()) {
                    visibleColumns.removeAll(hidden);
                }

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

        return Collections.singletonList(outputPath);
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
