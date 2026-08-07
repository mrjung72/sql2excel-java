package com.sql2excel.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class QueryConfig {
    private ExcelConfig excel;
    private Map<String, Object> vars;
    private Map<String, String> queryDefs;
    private List<DynamicVarConfig> dynamicVars;
    private List<SheetConfig> sheets;
    private List<SheetConfig> dynamicSheets;

    public QueryConfig() {
    }

    public ExcelConfig getExcel() {
        return excel;
    }

    public void setExcel(ExcelConfig excel) {
        this.excel = excel;
    }

    public Map<String, Object> getVars() {
        return vars;
    }

    public void setVars(Map<String, Object> vars) {
        this.vars = vars;
    }

    public Map<String, String> getQueryDefs() {
        return queryDefs;
    }

    public void setQueryDefs(Map<String, String> queryDefs) {
        this.queryDefs = queryDefs;
    }

    public List<DynamicVarConfig> getDynamicVars() {
        return dynamicVars;
    }

    public void setDynamicVars(List<DynamicVarConfig> dynamicVars) {
        this.dynamicVars = dynamicVars;
    }

    public List<SheetConfig> getSheets() {
        return sheets;
    }

    public void setSheets(List<SheetConfig> sheets) {
        this.sheets = sheets;
    }

    public List<SheetConfig> getDynamicSheets() {
        return dynamicSheets;
    }

    public void setDynamicSheets(List<SheetConfig> dynamicSheets) {
        this.dynamicSheets = dynamicSheets;
    }
}
