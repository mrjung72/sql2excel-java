package com.sql2excel.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class QueryResult {
    private final List<Map<String, Object>> rows;
    private final int rowCount;
    private final String query;
    private final Map<String, String> columnComments;
    private final List<String> columns;

    public QueryResult(List<Map<String, Object>> rows) {
        this(rows, null, null, null);
    }

    public QueryResult(List<Map<String, Object>> rows, String query) {
        this(rows, query, null, null);
    }

    public QueryResult(List<Map<String, Object>> rows, String query, Map<String, String> columnComments) {
        this(rows, query, columnComments, null);
    }

    public QueryResult(List<Map<String, Object>> rows, String query, Map<String, String> columnComments, List<String> columns) {
        this.rows = rows;
        this.rowCount = rows == null ? 0 : rows.size();
        this.query = query;
        this.columnComments = columnComments == null ? Collections.emptyMap() : columnComments;
        if (columns != null) {
            this.columns = columns;
        } else if (rows != null && !rows.isEmpty()) {
            this.columns = new ArrayList<>(rows.get(0).keySet());
        } else {
            this.columns = Collections.emptyList();
        }
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

    public Map<String, String> getColumnComments() {
        return columnComments;
    }

    public List<String> getColumns() {
        return columns;
    }
}
