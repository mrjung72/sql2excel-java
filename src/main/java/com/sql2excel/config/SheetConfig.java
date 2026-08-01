package com.sql2excel.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SheetConfig {
    private String name;
    @JsonProperty("use")
    private Boolean use = true;
    private String db;
    private String query;
    @JsonProperty("queryRef")
    private String queryRef;
    @JsonProperty("params")
    private Map<String, Object> params;
    @JsonProperty("aggregateColumn")
    private String aggregateColumn;
    @JsonProperty("exceptColumns")
    private String exceptColumns;
    @JsonProperty("hiddenColumns")
    private String hiddenColumns;
    @JsonProperty("maxRows")
    private Integer maxRows;
    private String style;
    private String dateColumnFormat;
    private Map<String, Object> header;
    private Map<String, Object> body;

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

    public String getQueryRef() {
        return queryRef;
    }

    public void setQueryRef(String queryRef) {
        this.queryRef = queryRef;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
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

    public String getHiddenColumns() {
        return hiddenColumns;
    }

    public void setHiddenColumns(String hiddenColumns) {
        this.hiddenColumns = hiddenColumns;
    }

    public Integer getMaxRows() {
        return maxRows;
    }

    public void setMaxRows(Integer maxRows) {
        this.maxRows = maxRows;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public String getDateColumnFormat() {
        return dateColumnFormat;
    }

    public void setDateColumnFormat(String dateColumnFormat) {
        this.dateColumnFormat = dateColumnFormat;
    }

    public Map<String, Object> getHeader() {
        return header;
    }

    public void setHeader(Map<String, Object> header) {
        this.header = header;
    }

    public Map<String, Object> getBody() {
        return body;
    }

    public void setBody(Map<String, Object> body) {
        this.body = body;
    }
}
