package com.sql2excel.query;

import java.util.List;
import java.util.Map;

public class QueryResult {
    private final List<Map<String, Object>> rows;
    private final int rowCount;

    public QueryResult(List<Map<String, Object>> rows) {
        this.rows = rows;
        this.rowCount = rows == null ? 0 : rows.size();
    }

    public List<Map<String, Object>> getRows() {
        return rows;
    }

    public int getRowCount() {
        return rowCount;
    }
}
