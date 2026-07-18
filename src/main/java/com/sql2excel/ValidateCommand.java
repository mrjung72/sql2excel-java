package com.sql2excel;

import com.sql2excel.config.*;
import picocli.CommandLine.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.Callable;

@Command(name = "validate", description = "Validate query definition file")
public class ValidateCommand implements Callable<Integer> {

    @Option(names = {"-q", "--query"}, description = "Query definition JSON file path")
    String queryPath;

    @Option(names = {"-x", "--xml"}, description = "Query definition XML file path")
    String xmlPath;

    @Option(names = {"-c", "--config"}, description = "DB config file path", defaultValue = "config/dbinfo.json")
    String configPath;

    @Override
    public Integer call() {
        if ((queryPath == null || queryPath.isEmpty()) && (xmlPath == null || xmlPath.isEmpty())) {
            System.err.println("Error: either --query or --xml must be specified.");
            return 1;
        }

        try {
            Path queryFile = Paths.get(queryPath != null ? queryPath : xmlPath);
            Path dbConfigFile = Paths.get(configPath);

            ConfigLoader loader = new ConfigLoader();
            Map<String, DatabaseConfig> databases = loader.loadDatabaseConfig(dbConfigFile);
            QueryConfig queryConfig = loader.loadQueryConfig(queryFile);

            if (queryConfig.getSheets() == null || queryConfig.getSheets().isEmpty()) {
                System.err.println("Error: no sheets defined.");
                return 1;
            }

            ExcelConfig excel = queryConfig.getExcel();
            boolean valid = true;
            for (SheetConfig sheet : queryConfig.getSheets()) {
                if (!Boolean.TRUE.equals(sheet.getUse())) {
                    continue;
                }
                if (sheet.getName() == null || sheet.getName().isEmpty()) {
                    System.err.println("Error: sheet name is empty.");
                    valid = false;
                } else if (sheet.getName().length() > 31) {
                    System.err.println("Warning: sheet name too long (max 31): " + sheet.getName());
                }
                if (sheet.getQuery() == null || sheet.getQuery().isEmpty()) {
                    System.err.println("Error: sheet '" + sheet.getName() + "' has no query.");
                    valid = false;
                }
                String dbKey = sheet.getDb() != null ? sheet.getDb() : (excel != null ? excel.getDb() : null);
                if (dbKey == null || !databases.containsKey(dbKey)) {
                    System.err.println("Error: database '" + dbKey + "' for sheet '" + sheet.getName() + "' not found.");
                    valid = false;
                }
            }

            if (valid) {
                System.out.println("Validation passed.");
                return 0;
            } else {
                System.out.println("Validation failed.");
                return 1;
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            return 1;
        }
    }
}
