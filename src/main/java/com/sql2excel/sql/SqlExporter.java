package com.sql2excel.sql;

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

public class SqlExporter {

    public List<String> export(String outputPath, List<ExcelExporter.SheetData> sheets) throws IOException {
        if (sheets == null || sheets.isEmpty()) {
            throw new IllegalArgumentException("No sheet data to export");
        }

        Path outPath = Paths.get(outputPath);
        Files.createDirectories(outPath.getParent());

        List<String> writtenFiles = new ArrayList<>();
        if (sheets.size() == 1) {
            writeSheet(outPath, sheets.get(0));
            writtenFiles.add(outPath.toString());
        } else {
            String baseName = outPath.getFileName().toString();
            int dotIndex = baseName.lastIndexOf('.');
            if (dotIndex > 0) {
                baseName = baseName.substring(0, dotIndex);
            }
            for (ExcelExporter.SheetData sheet : sheets) {
                String sheetFileName = baseName + "_" + sanitize(sheet.getName()) + ".sql";
                Path sheetPath = outPath.getParent().resolve(sheetFileName);
                writeSheet(sheetPath, sheet);
                writtenFiles.add(sheetPath.toString());
            }
        }
        return writtenFiles;
    }

    private void writeSheet(Path path, ExcelExporter.SheetData sheet) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(Files.newOutputStream(path), "UTF-8"))) {

            List<String> visibleColumns = new ArrayList<>(sheet.getColumns());
            List<String> hidden = sheet.getHiddenColumns();
            if (hidden != null && !hidden.isEmpty()) {
                visibleColumns.removeAll(hidden);
            }

            for (int i = 0; i < sheet.getRows().size(); i++) {
                if (visibleColumns.isEmpty()) {
                    continue;
                }
                // SQL 파일은 첫 번째 컬럼 값(statement)만 출력
                Object value = sheet.getRows().get(i).get(visibleColumns.get(0));
                if (value == null) {
                    continue;
                }
                Object formatted = ValueFormatter.formatValue(value, sheet.getDateColumnFormat());
                String sql = formatted == null ? "" : String.valueOf(formatted);
                writer.write(sql);
                if (!sql.trim().endsWith(";")) {
                    writer.write(";");
                }
                writer.newLine();
                writer.newLine();
            }
        }
    }

    private String sanitize(String name) {
        if (name == null) {
            return "Sheet";
        }
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
