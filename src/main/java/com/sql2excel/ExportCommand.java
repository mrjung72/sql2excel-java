package com.sql2excel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sql2excel.config.*;
import com.sql2excel.excel.ExcelExporter;
import com.sql2excel.query.QueryExecutor;
import com.sql2excel.query.QueryResult;
import com.sql2excel.variable.VariableResolver;
import picocli.CommandLine.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Callable;

@Command(name = "export", description = "Execute queries and export results to Excel")
public class ExportCommand implements Callable<Integer> {

    @Option(names = {"-c", "--config"}, description = "DB config file path", defaultValue = "config/dbinfo.json")
    String configPath;

    @Option(names = {"-q", "--query"}, description = "Query definition JSON file path")
    String queryPath;

    @Option(names = {"-x", "--xml"}, description = "Query definition XML file path")
    String xmlPath;

    @Option(names = {"-v", "--var"}, description = "Variables in key=value format (repeatable)", mapFallbackValue = Option.NULL_VALUE)
    Map<String, String> cliVars;

    @Override
    public Integer call() throws Exception {
        if ((queryPath == null || queryPath.isEmpty()) && (xmlPath == null || xmlPath.isEmpty())) {
            System.err.println("Error: either --query or --xml must be specified.");
            return 1;
        }

        Path queryFile = Paths.get(queryPath != null ? queryPath : xmlPath);
        Path dbConfigFile = Paths.get(configPath);

        ConfigLoader loader = new ConfigLoader();
        Map<String, DatabaseConfig> databases = loader.loadDatabaseConfig(dbConfigFile);
        QueryConfig queryConfig = loader.loadQueryConfig(queryFile);

        ExcelConfig excelConfig = queryConfig.getExcel();
        if (excelConfig == null || excelConfig.getOutput() == null || excelConfig.getOutput().isEmpty()) {
            System.err.println("Error: output path is required in excel config.");
            return 1;
        }

        Map<String, Object> vars = buildVariables(queryConfig.getVars(), cliVars);
        String outputPath = new VariableResolver().resolve(excelConfig.getOutput(), vars);

        List<ExcelExporter.SheetData> sheets = new ArrayList<>();
        Map<String, QueryExecutor> executors = new LinkedHashMap<>();

        try {
            for (SheetConfig sheet : queryConfig.getSheets()) {
                if (sheet.getUse() == null || !sheet.getUse()) {
                    continue;
                }
                if (sheet.getName() == null || sheet.getName().isEmpty()) {
                    System.err.println("Warning: skipping sheet without name.");
                    continue;
                }

                String dbKey = sheet.getDb() != null ? sheet.getDb() : excelConfig.getDb();
                if (dbKey == null) {
                    System.err.println("Error: no database specified for sheet " + sheet.getName());
                    return 1;
                }
                DatabaseConfig dbConfig = databases.get(dbKey);
                if (dbConfig == null) {
                    System.err.println("Error: database not found: " + dbKey);
                    return 1;
                }

                QueryExecutor executor = executors.computeIfAbsent(dbKey, k -> new QueryExecutor(dbConfig));
                QueryResult result = executor.run(sheet, vars, excelConfig.getMaxRows());

                List<String> columns = result.getRows().isEmpty()
                        ? Collections.emptyList()
                        : new ArrayList<>(result.getRows().get(0).keySet());
                columns = ExcelExporter.filterColumns(columns, sheet.getExceptColumns());

                List<Map<String, Object>> filteredRows = new ArrayList<>();
                for (Map<String, Object> row : result.getRows()) {
                    Map<String, Object> filtered = new LinkedHashMap<>();
                    for (String col : columns) {
                        filtered.put(col, row.get(col));
                    }
                    filteredRows.add(filtered);
                }

                String sheetName = new VariableResolver().resolve(sheet.getName(), vars);
                sheets.add(new ExcelExporter.SheetData(sheetName, columns, filteredRows));
                System.out.println("Sheet '" + sheetName + "' rows: " + result.getRowCount());
            }

            if (sheets.isEmpty()) {
                System.out.println("No sheets to export.");
                return 0;
            }

            new ExcelExporter().export(outputPath, sheets, excelConfig);
            System.out.println("Exported to: " + outputPath);
            return 0;
        } finally {
            for (QueryExecutor executor : executors.values()) {
                try {
                    executor.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private Map<String, Object> buildVariables(Map<String, Object> configVars, Map<String, String> cliVars) {
        Map<String, Object> merged = new LinkedHashMap<>();
        if (configVars != null) {
            merged.putAll(configVars);
        }
        if (cliVars != null) {
            for (Map.Entry<String, String> entry : cliVars.entrySet()) {
                String value = entry.getValue();
                merged.put(entry.getKey(), parseVarValue(value));
            }
        }
        return merged;
    }

    private Object parseVarValue(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if ((trimmed.startsWith("[") && trimmed.endsWith("]")) ||
                (trimmed.startsWith("{") && trimmed.endsWith("}"))) {
            try {
                return new ObjectMapper().readValue(trimmed, Object.class);
            } catch (Exception ignored) {
            }
        }
        return value;
    }
}
