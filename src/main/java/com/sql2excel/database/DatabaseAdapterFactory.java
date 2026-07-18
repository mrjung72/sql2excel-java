package com.sql2excel.database;

import com.sql2excel.config.DatabaseConfig;

public class DatabaseAdapterFactory {

    public static DatabaseAdapter createAdapter(DatabaseConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Database config is required");
        }
        String type = config.getType();
        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException("Database type is required");
        }
        // In the future other adapter families can be routed here (e.g. ODBC bridge).
        return new JdbcDatabaseAdapter(config);
    }
}
