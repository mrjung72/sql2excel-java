package com.sql2excel.query;

import java.util.List;
import java.util.Map;

public class QueryResult {
    private final List<Map<String, Object>> rows;
    private final int rowCount;
    private final String query;

    public QueryResult(List<Map<String, Object>> rows) {
        this(rows, null);
    }

    public QueryResult(List<Map<String, Object>> rows, String query) {
        this.rows = rows;
        this.rowCount = rows == null ? 0 : rows.size();
        this.query = query;
    }

    public List<Map<String, Object>> getRows() {
        return rows;
    }

    public int getRowCount() {
        return rowCount;
    }

    public String getQuery() {
        return query;
    }
}
