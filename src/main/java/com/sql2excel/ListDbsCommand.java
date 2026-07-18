package com.sql2excel;

import com.sql2excel.config.ConfigLoader;
import com.sql2excel.config.DatabaseConfig;
import com.sql2excel.database.DatabaseAdapter;
import com.sql2excel.database.DatabaseAdapterFactory;
import picocli.CommandLine.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.Callable;

@Command(name = "list-dbs", description = "Show database list and connection status")
public class ListDbsCommand implements Callable<Integer> {

    @Option(names = {"-c", "--config"}, description = "DB config file path", defaultValue = "config/dbinfo.json")
    String configPath;

    @Override
    public Integer call() {
        try {
            Map<String, DatabaseConfig> databases = new ConfigLoader()
                    .loadDatabaseConfig(Paths.get(configPath));

            if (databases.isEmpty()) {
                System.out.println("No databases configured.");
                return 0;
            }

            System.out.println("Testing " + databases.size() + " database connection(s):");
            int success = 0;
            int failed = 0;
            for (Map.Entry<String, DatabaseConfig> entry : databases.entrySet()) {
                String dbKey = entry.getKey();
                DatabaseConfig config = entry.getValue();
                DatabaseAdapter adapter = DatabaseAdapterFactory.createAdapter(config);
                try {
                    boolean ok = adapter.testConnection();
                    System.out.println("  " + dbKey + ": " + (ok ? "OK" : "FAILED"));
                    if (ok) {
                        success++;
                    } else {
                        failed++;
                    }
                } catch (Exception e) {
                    failed++;
                    System.out.println("  " + dbKey + ": FAILED - " + e.getMessage());
                } finally {
                    try {
                        adapter.close();
                    } catch (Exception ignored) {
                    }
                }
            }
            System.out.println("Success: " + success + ", Failed: " + failed);
            return 0;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }
}
