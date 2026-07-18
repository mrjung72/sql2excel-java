package com.sql2excel.query;

import com.sql2excel.config.DatabaseConfig;
import com.sql2excel.config.SheetConfig;
import com.sql2excel.database.DatabaseAdapter;
import com.sql2excel.database.DatabaseAdapterFactory;
import com.sql2excel.database.DatabaseType;
import com.sql2excel.variable.VariableResolver;

import java.sql.SQLException;
import java.util.Map;

public class QueryExecutor {

    private final DatabaseConfig config;
    private final DatabaseAdapter adapter;
    private final VariableResolver variableResolver;

    public QueryExecutor(DatabaseConfig config) {
        this.config = config;
        this.adapter = DatabaseAdapterFactory.createAdapter(config);
        this.variableResolver = new VariableResolver();
    }

    public QueryResult run(SheetConfig sheet, Map<String, Object> vars, Integer globalMaxRows) throws Exception {
        ensureConnected();
        String sql = sheet.getQuery();
        if (sql == null || sql.isEmpty()) {
            throw new IllegalArgumentException("Sheet query is empty: " + sheet.getName());
        }

        sql = variableResolver.resolveSql(sql, vars);
        sql = applyDatabaseDialect(sql);

        Integer maxRows = sheet.getMaxRows() != null ? sheet.getMaxRows() : globalMaxRows;

        return adapter.executeQuery(sql, maxRows);
    }

    private String applyDatabaseDialect(String sql) {
        DatabaseType type = DatabaseType.fromString(config.getType());
        String transformed = type.replaceGetDate(sql);
        return transformed;
    }

    public void close() throws Exception {
        if (adapter != null) {
            adapter.close();
        }
    }

    private void ensureConnected() throws Exception {
        adapter.connect();
    }
}
