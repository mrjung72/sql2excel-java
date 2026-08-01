package com.sql2excel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sql2excel.config.DatabaseConfig;
import com.sql2excel.config.DynamicVarConfig;
import com.sql2excel.config.ExcelConfig;
import com.sql2excel.config.QueryConfig;
import com.sql2excel.config.SheetConfig;
import com.sql2excel.database.DatabaseAdapter;
import com.sql2excel.database.DatabaseAdapterFactory;
import com.sql2excel.database.DatabaseType;
import com.sql2excel.excel.ExcelExporter;
import com.sql2excel.query.QueryExecutor;
import com.sql2excel.query.QueryResult;
import com.sql2excel.variable.VariableResolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ExportService {

    public static int export(QueryConfig queryConfig,
                             Map<String, DatabaseConfig> databases,
                             Map<String, Object> vars,
                             String outputPath) throws Exception {
        ExcelConfig excelConfig = queryConfig.getExcel();
        if (excelConfig == null) {
            throw new IllegalStateException("excel config is missing");
        }

        if (!checkAllDatabaseConnections(queryConfig, databases)) {
            return 1;
        }

        List<ExcelExporter.SheetData> sheets = new ArrayList<>();
        List<Map<String, Object>> tocRows = new ArrayList<>();
        Map<String, QueryExecutor> executors = new LinkedHashMap<>();

        try {
            resolveDynamicVars(queryConfig.getDynamicVars(), databases,
                    excelConfig.getDb() != null ? excelConfig.getDb() : null, vars);

            List<SheetConfig> sheetConfigs = queryConfig.getSheets();
            if (sheetConfigs == null || sheetConfigs.isEmpty()) {
                System.out.println("No sheets to export.");
                return 0;
            }
            for (SheetConfig sheet : sheetConfigs) {
                if (sheet == null) {
                    continue;
                }
                if (sheet.getUse() == null || !sheet.getUse()) {
                    continue;
                }
                if (sheet.getName() == null || sheet.getName().isEmpty()) {
                    System.err.println("Warning: skipping sheet without name.");
                    continue;
                }

                String dbKey = sheet.getDb() != null ? sheet.getDb() : excelConfig.getDb();
                if (dbKey == null) {
                    throw new IllegalStateException("no database specified for sheet " + sheet.getName());
                }
                DatabaseConfig dbConfig = databases.get(dbKey);
                if (dbConfig == null) {
                    throw new IllegalStateException("database not found: " + dbKey);
                }

                Map<String, Object> sheetVars = new LinkedHashMap<>(vars);
                if (sheet.getParams() != null) {
                    sheetVars.putAll(sheet.getParams());
                }

                QueryExecutor executor = executors.computeIfAbsent(dbKey, k -> new QueryExecutor(dbConfig));
                QueryResult result = executor.run(sheet, sheetVars, excelConfig.getMaxRows());

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

                String sheetName = new VariableResolver().resolve(sheet.getName(), sheetVars);
                Map<String, Object> header = sheet.getHeader() != null ? sheet.getHeader() : excelConfig.getHeader();
                Map<String, Object> body = sheet.getBody() != null ? sheet.getBody() : excelConfig.getBody();
                List<String> hiddenColumns = parseColumns(sheet.getHiddenColumns());
                String dateColumnFormat = sheet.getDateColumnFormat() != null ? sheet.getDateColumnFormat() : excelConfig.getDateColumnFormat();
                sheets.add(new ExcelExporter.SheetData(sheetName, columns, filteredRows, header, body, hiddenColumns, dateColumnFormat));

                Map<String, Object> tocRow = new LinkedHashMap<>();
                tocRow.put("시트명", sheetName);
                tocRow.put("조회건수", result.getRowCount());
                tocRow.put("사용된 SQL문", result.getQuery());
                tocRows.add(tocRow);

                System.out.println("Sheet '" + sheetName + "' rows: " + result.getRowCount());
            }

            if (sheets.isEmpty()) {
                System.out.println("No sheets to export.");
                return 0;
            }

            List<String> tocColumns = Arrays.asList("시트명", "조회건수", "사용된 SQL문");
            Map<String, Object> tocHeader = excelConfig.getHeader();
            Map<String, Object> tocBody = new HashMap<>();
            if (excelConfig.getBody() != null) {
                tocBody.putAll(excelConfig.getBody());
            }
            tocBody.put("wrap", true);
            sheets.add(0, new ExcelExporter.SheetData("목차", tocColumns, tocRows, tocHeader, tocBody));

            new ExcelExporter().export(outputPath, sheets);
            System.out.println("Exported to: " + outputPath);
            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            for (QueryExecutor executor : executors.values()) {
                try {
                    executor.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    public static Map<String, Object> buildVariables(Map<String, Object> configVars, Map<String, String> cliVars) {
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

    private static Object parseVarValue(String value) {
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

    private static boolean checkAllDatabaseConnections(QueryConfig queryConfig,
                                                       Map<String, DatabaseConfig> databases) {
        ExcelConfig excelConfig = queryConfig.getExcel();
        String defaultDb = excelConfig != null ? excelConfig.getDb() : null;

        Set<String> dbKeys = new LinkedHashSet<>();
        if (defaultDb != null && !defaultDb.isEmpty()) {
            dbKeys.add(defaultDb);
        }

        List<DynamicVarConfig> dynamicVars = queryConfig.getDynamicVars();
        if (dynamicVars != null) {
            for (DynamicVarConfig dv : dynamicVars) {
                if (dv == null) {
                    continue;
                }
                String db = dv.getDb() != null ? dv.getDb() : defaultDb;
                if (db != null && !db.isEmpty()) {
                    dbKeys.add(db);
                }
            }
        }

        List<SheetConfig> sheetConfigs = queryConfig.getSheets();
        if (sheetConfigs != null) {
            for (SheetConfig sheet : sheetConfigs) {
                if (sheet == null) {
                    continue;
                }
                if (sheet.getUse() == null || !sheet.getUse()) {
                    continue;
                }
                String db = sheet.getDb() != null ? sheet.getDb() : defaultDb;
                if (db != null && !db.isEmpty()) {
                    dbKeys.add(db);
                }
            }
        }

        boolean allOk = true;
        for (String dbKey : dbKeys) {
            DatabaseConfig dbConfig = databases.get(dbKey);
            if (dbConfig == null) {
                System.out.println("[DB Check] " + dbKey + " ... NOT FOUND (dbinfo.json)");
                allOk = false;
                continue;
            }
            DatabaseAdapter adapter = null;
            try {
                adapter = DatabaseAdapterFactory.createAdapter(dbConfig);
                boolean ok = adapter.testConnection();
                String type = dbConfig.getType() != null ? dbConfig.getType() : "?";
                if (ok) {
                    System.out.println("[DB Check] " + dbKey + " (" + type + ") ... OK");
                } else {
                    System.out.println("[DB Check] " + dbKey + " (" + type + ") ... FAIL");
                    allOk = false;
                }
            } catch (Exception e) {
                String type = dbConfig.getType() != null ? dbConfig.getType() : "?";
                System.out.println("[DB Check] " + dbKey + " (" + type + ") ... FAIL - " + e.getMessage());
                allOk = false;
            } finally {
                if (adapter != null) {
                    try {
                        adapter.close();
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        return allOk;
    }

    private static List<String> parseColumns(String columns) {
        if (columns == null || columns.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (String s : columns.split(",")) {
            String trimmed = s.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private static void resolveDynamicVars(List<DynamicVarConfig> dynamicVars,
                                          Map<String, DatabaseConfig> databases,
                                          String defaultDb,
                                          Map<String, Object> vars) throws Exception {
        if (dynamicVars == null || dynamicVars.isEmpty()) {
            return;
        }
        Map<String, DatabaseAdapter> adapters = new LinkedHashMap<>();
        try {
            for (DynamicVarConfig dv : dynamicVars) {
                String dbKey = dv.getDb() != null ? dv.getDb() : defaultDb;
                if (dbKey == null) {
                    throw new IllegalStateException("no database specified for dynamic var: " + dv.getName());
                }
                DatabaseConfig dbConfig = databases.get(dbKey);
                if (dbConfig == null) {
                    throw new IllegalStateException("database not found for dynamic var: " + dbKey);
                }
                DatabaseAdapter adapter = adapters.computeIfAbsent(dbKey, k -> DatabaseAdapterFactory.createAdapter(dbConfig));
                if (adapter == null) {
                    throw new IllegalStateException("failed to create adapter for dynamic var: " + dbKey);
                }
                if (!adapter.testConnection()) {
                    adapter.connect();
                }

                DatabaseType type = DatabaseType.fromString(dbConfig.getType());
                String sql = type.replaceGetDate(dv.getQuery());
                QueryResult result = adapter.executeQuery(sql, null);

                if (result == null || result.getRows() == null || result.getRows().isEmpty()) {
                    continue;
                }

                List<String> columns = new ArrayList<>(result.getRows().get(0).keySet());
                Map<String, List<Object>> columnLists = new LinkedHashMap<>();
                for (String col : columns) {
                    columnLists.put(col, new ArrayList<>());
                }
                for (Map<String, Object> row : result.getRows()) {
                    for (String col : columns) {
                        columnLists.get(col).add(row.get(col));
                    }
                }

                if ("key_value_pairs".equalsIgnoreCase(dv.getType()) && columns.size() >= 2) {
                    String firstKey = columns.get(0);
                    String secondKey = columns.get(1);
                    List<Object> firstList = columnLists.get(firstKey);
                    List<Object> secondList = columnLists.get(secondKey);
                    for (int i = 0; i < firstList.size() && i < secondList.size(); i++) {
                        Object key = firstList.get(i);
                        if (key != null) {
                            String varName = dv.getName() + "." + key.toString().trim();
                            vars.put(varName, secondList.get(i));
                        }
                    }
                }

                for (Map.Entry<String, List<Object>> entry : columnLists.entrySet()) {
                    vars.put(dv.getName() + "." + entry.getKey(), entry.getValue());
                }
            }
        } finally {
            for (DatabaseAdapter adapter : adapters.values()) {
                try {
                    adapter.close();
                } catch (Exception ignored) {
                }
            }
        }
    }
}
