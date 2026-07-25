package com.sql2excel;

import com.sql2excel.config.StyleTemplate;
import com.sql2excel.excel.ExcelExporter;
import picocli.CommandLine.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@Command(name = "export-style-samples", description = "Create a sample Excel workbook with one sheet per defined style")
public class ExportStyleSamplesCommand implements Callable<Integer> {

    @Option(names = {"-s", "--styles"}, description = "Excel style definition file", defaultValue = "config/excel-style.json")
    String stylesPath;

    @Option(names = {"-o", "--output"}, description = "Output sample workbook path", defaultValue = "output/style-samples.xlsx")
    String outputPath;

    @Override
    public Integer call() throws Exception {
        if (stylesPath == null || stylesPath.isEmpty()) {
            stylesPath = "config/excel-style.json";
        }
        if (outputPath == null || outputPath.isEmpty()) {
            outputPath = "output/style-samples.xlsx";
        }

        Path styleFile = Paths.get(stylesPath);
        StyleTemplate.load(styleFile);

        List<String> styleNames = StyleTemplate.getStyleNames();
        if (styleNames.isEmpty()) {
            System.err.println("Error: no styles found in " + stylesPath);
            return 1;
        }

        List<String> sampleColumns = Arrays.asList("No", "Name", "Value", "Date");
        List<Map<String, Object>> sampleRows = createSampleRows();

        List<ExcelExporter.SheetData> sheets = new ArrayList<>();
        List<String> tocColumns = Arrays.asList("시트명", "설명");
        List<Map<String, Object>> tocRows = new ArrayList<>();

        for (String styleName : styleNames) {
            Map<String, Object> header = copy(StyleTemplate.getStyleHeader(styleName));
            Map<String, Object> body = copy(StyleTemplate.getStyleBody(styleName));

            sheets.add(new ExcelExporter.SheetData(styleName, sampleColumns, sampleRows, header, body));

            Map<String, Object> tocRow = new LinkedHashMap<>();
            tocRow.put("시트명", styleName);
            tocRow.put("설명", "style: " + styleName);
            tocRows.add(tocRow);
        }

        Map<String, Object> tocBody = new HashMap<>();
        tocBody.put("wrap", true);
        sheets.add(0, new ExcelExporter.SheetData("목차", tocColumns, tocRows, null, tocBody));

        try {
            new ExcelExporter().export(outputPath, sheets);
            System.out.println("Sample workbook exported to: " + outputPath);
            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    private List<Map<String, Object>> createSampleRows() {
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(createRow(1, "Alice", 100, LocalDate.of(2024, 1, 15)));
        rows.add(createRow(2, "Bob", 250, LocalDate.of(2024, 2, 20)));
        rows.add(createRow(3, "Charlie", 175, LocalDate.of(2024, 3, 25)));
        rows.add(createRow(4, "Diana", 320, LocalDate.of(2024, 4, 30)));
        return rows;
    }

    private Map<String, Object> createRow(int no, String name, int value, LocalDate date) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("No", no);
        row.put("Name", name);
        row.put("Value", value);
        row.put("Date", date);
        return row;
    }

    private Map<String, Object> copy(Map<String, Object> map) {
        return map == null ? null : new HashMap<>(map);
    }
}
