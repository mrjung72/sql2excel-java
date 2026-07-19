package com.sql2excel;

import com.sql2excel.config.ConfigLoader;
import com.sql2excel.config.DatabaseConfig;
import com.sql2excel.database.DatabaseAdapter;
import com.sql2excel.database.DatabaseAdapterFactory;
import picocli.CommandLine.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@Command(name = "list-dbs", description = "Show database list and connection status")
public class ListDbsCommand implements Callable<Integer> {

    @Option(names = {"-c", "--config"}, description = "DB config file path", defaultValue = "config/dbinfo.json")
    String configPath;

    private static final int MIN_KEY_WIDTH = 12;
    private static final int MIN_TYPE_WIDTH = 8;
    private static final int MIN_TARGET_WIDTH = 24;
    private static final int STATUS_WIDTH = 8;
    private static final int TIME_WIDTH = 19;
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Integer call() {
        try {
            Map<String, DatabaseConfig> databases = new ConfigLoader()
                    .loadDatabaseConfig(Paths.get(configPath));

            if (databases.isEmpty()) {
                System.out.println("No databases configured.");
                return 0;
            }

            List<Map.Entry<String, DatabaseConfig>> entries = new ArrayList<>(databases.entrySet());
            List<String> targets = new ArrayList<>(entries.size());

            int keyWidth = MIN_KEY_WIDTH;
            int typeWidth = MIN_TYPE_WIDTH;
            int targetWidth = MIN_TARGET_WIDTH;
            int timeWidth = TIME_WIDTH;

            for (Map.Entry<String, DatabaseConfig> entry : entries) {
                DatabaseConfig config = entry.getValue();
                String target = getTarget(config);
                String type = config.getType();

                targets.add(target);
                keyWidth = Math.max(keyWidth, entry.getKey().length());
                typeWidth = Math.max(typeWidth, type == null ? 0 : type.length());
                targetWidth = Math.max(targetWidth, target.length());
            }

            String rowFormat = "%-3s  %-" + keyWidth + "s  %-" + typeWidth + "s  %-" + targetWidth + "s  %-" + timeWidth + "s  %-" + STATUS_WIDTH + "s";
            int lineLength = 3 + 2 + keyWidth + 2 + typeWidth + 2 + targetWidth + 2 + timeWidth + 2 + STATUS_WIDTH;
            String separator = "-".repeat(lineLength);

            LocalDateTime now = LocalDateTime.now();

            System.out.println();
            System.out.println("Database Connection Test Results (" + now.format(TIMESTAMP_FORMATTER) + ")");
            System.out.println(separator);
            System.out.println(String.format(rowFormat, "#", "Database Key", "Type", "Target", "Time", "Status"));
            System.out.println(separator);

            int success = 0;
            int failed = 0;
            for (int i = 0; i < entries.size(); i++) {
                Map.Entry<String, DatabaseConfig> entry = entries.get(i);
                String dbKey = entry.getKey();
                DatabaseConfig config = entry.getValue();
                String type = config.getType() == null ? "" : config.getType();
                String target = targets.get(i);
                DatabaseAdapter adapter = DatabaseAdapterFactory.createAdapter(config);

                String status;
                String errorDetail = null;
                try {
                    boolean ok = adapter.testConnection();
                    if (ok) {
                        status = "[OK]";
                        success++;
                    } else {
                        status = "[FAILED]";
                        errorDetail = "connection test returned false";
                        failed++;
                    }
                } catch (Exception e) {
                    status = "[FAILED]";
                    errorDetail = e.getMessage();
                    if (errorDetail == null || errorDetail.isEmpty()) {
                        errorDetail = e.getClass().getSimpleName();
                    }
                    failed++;
                } finally {
                    try {
                        adapter.close();
                    } catch (Exception ignored) {
                    }
                }

                LocalDateTime rowTime = LocalDateTime.now();
                System.out.println(String.format(rowFormat, (i + 1) + ".", dbKey, type, target, rowTime.format(TIMESTAMP_FORMATTER), status));
                if (errorDetail != null) {
                    System.out.println("     -> " + errorDetail);
                }
            }

            System.out.println(separator);
            System.out.println("Total: " + entries.size() + "  [OK] " + success + "  [FAILED] " + failed);

            return failed == 0 ? 0 : 1;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }

    private String getTarget(DatabaseConfig config) {
        String type = config.getType();

        if (type != null && type.equalsIgnoreCase("sqlite")) {
            if (config.getConnectString() != null && !config.getConnectString().isEmpty()) {
                return config.getConnectString();
            }
            if (config.getDatabase() != null && !config.getDatabase().isEmpty()) {
                return config.getDatabase();
            }
            if (config.getServer() != null && !config.getServer().isEmpty()) {
                return config.getServer();
            }
            return ":memory:";
        }

        String host = config.getServer() != null && !config.getServer().isEmpty() ? config.getServer() : "localhost";
        int port = config.getPort();

        String dbId = null;
        String separator = "/";
        if (config.getServiceName() != null && !config.getServiceName().isEmpty()) {
            dbId = config.getServiceName();
            if (isTibero(type)) {
                separator = ":";
            }
        } else if (config.getSid() != null && !config.getSid().isEmpty()) {
            dbId = config.getSid();
            separator = ":";
        } else if (config.getDatabase() != null && !config.getDatabase().isEmpty()) {
            dbId = config.getDatabase();
            if (isTibero(type)) {
                separator = ":";
            }
        }

        if (dbId == null || dbId.isEmpty()) {
            return port > 0 ? host + ":" + port : host;
        }
        return port > 0 ? host + ":" + port + separator + dbId : host + separator + dbId;
    }

    private boolean isTibero(String type) {
        return type != null && type.equalsIgnoreCase("tibero");
    }
}
