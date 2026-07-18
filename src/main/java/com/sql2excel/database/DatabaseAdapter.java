package com.sql2excel.database;

import com.sql2excel.query.QueryResult;

import java.sql.SQLException;

public interface DatabaseAdapter {
    String getType();
    void connect() throws Exception;
    void close() throws Exception;
    boolean testConnection() throws Exception;
    QueryResult executeQuery(String sql, Integer maxRows) throws SQLException;
}
