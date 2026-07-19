package com.sql2excel.database;

import com.sql2excel.config.DatabaseConfig;
import com.sql2excel.query.QueryResult;

import java.sql.*;
import java.util.*;

public class JdbcDatabaseAdapter implements DatabaseAdapter {

    private final DatabaseConfig config;
    private final DatabaseType type;
    private Connection connection;

    public JdbcDatabaseAdapter(DatabaseConfig config) {
        this.config = config;
        this.type = DatabaseType.fromString(config.getType());
    }

    @Override
    public String getType() {
        return type.name().toLowerCase();
    }

    @Override
    public void connect() throws Exception {
        if (connection != null && !connection.isClosed()) {
            return;
        }
        String driverClass = config.getDriverClass();
        if (driverClass == null || driverClass.isEmpty()) {
            driverClass = type.getDefaultDriverClass();
        }

        DriverLoader.loadDriver(config.getJar(), driverClass);

        String url = type.buildJdbcUrl(config);

        String user = config.getUser();
        String password = config.getPassword();

        if (user != null && !user.isEmpty() && password != null) {
            this.connection = DriverManager.getConnection(url, user, password);
        } else {
            this.connection = DriverManager.getConnection(url);
        }
    }

    @Override
    public boolean testConnection() throws Exception {
        if (connection == null || !connection.isValid(5)) {
            connect();
        }
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(type.getTestQuery())) {
            return rs.next();
        }
    }

    @Override
    public QueryResult executeQuery(String sql, Integer maxRows) throws SQLException {
        if (connection == null || connection.isClosed()) {
            throw new SQLException("Connection is not open");
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        try (Statement stmt = connection.createStatement()) {
            if (maxRows != null && maxRows > 0) {
                stmt.setMaxRows(maxRows);
            }
            try (ResultSet rs = stmt.executeQuery(sql)) {
                ResultSetMetaData meta = rs.getMetaData();
                int columnCount = meta.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.put(meta.getColumnLabel(i), rs.getObject(i));
                    }
                    rows.add(row);
                }
            }
        }
        return new QueryResult(rows);
    }

    @Override
    public void close() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
            connection = null;
        }
    }

    public DatabaseType getDatabaseType() {
        return type;
    }

    public DatabaseConfig getConfig() {
        return config;
    }
}
