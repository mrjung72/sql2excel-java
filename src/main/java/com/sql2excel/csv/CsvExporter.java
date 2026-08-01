package com.sql2excel.csv;

import com.sql2excel.excel.ExcelExporter;
import com.sql2excel.export.ValueFormatter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CsvExporter {

    public List<String> export(String outputPath, List<ExcelExporter.SheetData> sheets) throws IOException {
        if (sheets == null || sheets.isEmpty()) {
            throw new IllegalArgumentException("No sheet data to export");
        }

        String ext = getExtension(outputPath).toLowerCase();
        char delimiter = "txt".equals(ext) ? '\t' : ',';
        boolean writeBom = "csv".equals(ext);

        Path outPath = Paths.get(outputPath);
        Files.createDirectories(outPath.getParent());

        List<String> writtenFiles = new ArrayList<>();
        if (sheets.size() == 1) {
            writeSheet(outPath, sheets.get(0), delimiter, writeBom);
            writtenFiles.add(outPath.toString());
        } else {
            String baseName = outPath.getFileName().toString();
            int dotIndex = baseName.lastIndexOf('.');
            if (dotIndex > 0) {
                baseName = baseName.substring(0, dotIndex);
            }
            for (ExcelExporter.SheetData sheet : sheets) {
                String sheetFileName = baseName + "_" + sanitize(sheet.getName()) + "." + ext;
                Path sheetPath = outPath.getParent().resolve(sheetFileName);
                writeSheet(sheetPath, sheet, delimiter, writeBom);
                writtenFiles.add(sheetPath.toString());
            }
        }
        return writtenFiles;
    }

    private void writeSheet(Path path, ExcelExporter.SheetData sheet, char delimiter, boolean writeBom) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(Files.newOutputStream(path), "UTF-8"))) {
            if (writeBom) {
                writer.write('\ufeff');
            }

            List<String> columns = new ArrayList<>(sheet.getColumns());
            List<String> hidden = sheet.getHiddenColumns();
            if (hidden != null && !hidden.isEmpty()) {
                columns.removeAll(hidden);
            }

            // header
            for (int i = 0; i < columns.size(); i++) {
                if (i > 0) {
                    writer.write(delimiter);
                }
                writer.write(escape(columns.get(i), delimiter));
            }
            writer.newLine();

            // rows
            for (Map<String, Object> row : sheet.getRows()) {
                for (int i = 0; i < columns.size(); i++) {
                    if (i > 0) {
                        writer.write(delimiter);
                    }
                    Object value = row.get(columns.get(i));
                    Object formatted = ValueFormatter.formatValue(value, sheet.getDateColumnFormat());
                    writer.write(escape(formatted == null ? "" : String.valueOf(formatted), delimiter));
                }
                writer.newLine();
            }
        }
    }

    private String escape(String value, char delimiter) {
        if (value == null) {
            return "";
        }
        if (value.indexOf(delimiter) >= 0
                || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0
                || value.indexOf('\r') >= 0) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String getExtension(String path) {
        int dot = path.lastIndexOf('.');
        return dot >= 0 ? path.substring(dot + 1) : "csv";
    }

    private String sanitize(String name) {
        if (name == null) {
            return "Sheet";
        }
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
