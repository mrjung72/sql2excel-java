package com.sql2excel.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SheetConfig {
    private String name;
    @JsonProperty("use")
    private Boolean use = true;
    private String db;
    private String query;
    @JsonProperty("aggregateColumn")
    private String aggregateColumn;
    @JsonProperty("exceptColumns")
    private String exceptColumns;
    @JsonProperty("maxRows")
    private Integer maxRows;

    public SheetConfig() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getUse() {
        return use;
    }

    public void setUse(Boolean use) {
        this.use = use;
    }

    public String getDb() {
        return db;
    }

    public void setDb(String db) {
        this.db = db;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getAggregateColumn() {
        return aggregateColumn;
    }

    public void setAggregateColumn(String aggregateColumn) {
        this.aggregateColumn = aggregateColumn;
    }

    public String getExceptColumns() {
        return exceptColumns;
    }

    public void setExceptColumns(String exceptColumns) {
        this.exceptColumns = exceptColumns;
    }

    public Integer getMaxRows() {
        return maxRows;
    }

    public void setMaxRows(Integer maxRows) {
        this.maxRows = maxRows;
    }
}
