package com.sql2excel.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class QueryConfig {
    private ExcelConfig excel;
    private Map<String, Object> vars;
    private List<SheetConfig> sheets;

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

    public List<SheetConfig> getSheets() {
        return sheets;
    }

    public void setSheets(List<SheetConfig> sheets) {
        this.sheets = sheets;
    }
}
