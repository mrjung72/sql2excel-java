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
            throw new IllegalArgumentException("driverClass is not defined in dbinfo.json for type: " + type);
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
        return executeQuery(sql, maxRows, false);
    }

    @Override
    public QueryResult executeQuery(String sql, Integer maxRows, boolean fetchComments) throws SQLException {
        if (connection == null || connection.isClosed()) {
            throw new SQLException("Connection is not open");
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, String> columnComments = new LinkedHashMap<>();
        List<String> columnLabels = new ArrayList<>();
        try (Statement stmt = connection.createStatement()) {
            if (maxRows != null && maxRows > 0) {
                stmt.setMaxRows(maxRows);
            }
            try (ResultSet rs = stmt.executeQuery(sql)) {
                ResultSetMetaData meta = rs.getMetaData();
                int columnCount = meta.getColumnCount();
                for (int i = 1; i <= columnCount; i++) {
                    columnLabels.add(meta.getColumnLabel(i));
                }
                if (fetchComments) {
                    columnComments = fetchColumnComments(meta);
                }
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.put(meta.getColumnLabel(i), rs.getObject(i));
                    }
                    rows.add(row);
                }
            }
        }
        return new QueryResult(rows, null, columnComments, columnLabels);
    }

    private Map<String, String> fetchColumnComments(ResultSetMetaData meta) throws SQLException {
        Map<String, String> comments = new LinkedHashMap<>();
        DatabaseMetaData dbMeta = connection.getMetaData();
        int count = meta.getColumnCount();
        for (int i = 1; i <= count; i++) {
            String label = meta.getColumnLabel(i);
            String tableName = meta.getTableName(i);
            String columnName = meta.getColumnName(i);
            String comment = lookupColumnComment(dbMeta, tableName, columnName);
            comments.put(label, comment);
        }
        return comments;
    }

    private String lookupColumnComment(DatabaseMetaData dbMeta, String tableName, String columnName) throws SQLException {
        if (tableName == null || tableName.isEmpty() || columnName == null || columnName.isEmpty()) {
            return null;
        }
        String[] tableCandidates = new String[]{tableName, tableName.toLowerCase(), tableName.toUpperCase()};
        String[] columnCandidates = new String[]{columnName, columnName.toLowerCase(), columnName.toUpperCase()};
        for (String table : tableCandidates) {
            for (String column : columnCandidates) {
                try (ResultSet rs = dbMeta.getColumns(null, null, table, column)) {
                    if (rs.next()) {
                        String remarks = rs.getString("REMARKS");
                        if (remarks != null && !remarks.isEmpty()) {
                            return remarks;
                        }
                    }
                }
            }
        }
        return null;
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
