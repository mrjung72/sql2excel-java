package com.sql2excel.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sql2excel.excel.ExcelExporter;
import com.sql2excel.export.ValueFormatter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JsonExporter {

    public List<String> export(String outputPath, List<ExcelExporter.SheetData> sheets) throws IOException {
        if (sheets == null || sheets.isEmpty()) {
            throw new IllegalArgumentException("No sheet data to export");
        }

        Files.createDirectories(Paths.get(outputPath).getParent());

        Map<String, List<Map<String, Object>>> data = new LinkedHashMap<>();
        for (ExcelExporter.SheetData sheet : sheets) {
            List<String> visibleColumns = new ArrayList<>(sheet.getColumns());
            List<String> hidden = sheet.getHiddenColumns();
            if (hidden != null && !hidden.isEmpty()) {
                visibleColumns.removeAll(hidden);
            }

            List<Map<String, Object>> rows = new ArrayList<>();
            for (Map<String, Object> row : sheet.getRows()) {
                Map<String, Object> jsonRow = new LinkedHashMap<>();
                for (String col : visibleColumns) {
                    jsonRow.put(col, ValueFormatter.formatValue(row.get(col), sheet.getDateColumnFormat()));
                }
                rows.add(jsonRow);
            }
            data.put(sheet.getName(), rows);
        }

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(new File(outputPath), data);

        return Collections.singletonList(outputPath);
    }
}
