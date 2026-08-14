package com.sql2excel.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ConfigLoader {

    private final ObjectMapper objectMapper;

    public ConfigLoader() {
        this.objectMapper = new ObjectMapper()
                .configure(JsonParser.Feature.ALLOW_COMMENTS, true)
                .configure(JsonParser.Feature.ALLOW_YAML_COMMENTS, true)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public Map<String, DatabaseConfig> loadDatabaseConfig(Path path) throws IOException {
        String content = Files.readString(path, StandardCharsets.UTF_8);
        MapType mapType = objectMapper.getTypeFactory()
                .constructMapType(LinkedHashMap.class, String.class, DatabaseConfig.class);
        return objectMapper.readValue(content, mapType);
    }

    public QueryConfig loadQueryConfig(Path path) throws IOException {
        StyleTemplate.load(Path.of("config/excel-style.json"));
        String name = path.getFileName().toString().toLowerCase();
        if (name.endsWith(".json")) {
            QueryConfig qc = objectMapper.readValue(path.toFile(), QueryConfig.class);
            applyStyles(qc);
            return qc;
        }
        if (name.endsWith(".xml")) {
            return loadQueryConfigXml(path);
        }
        throw new IOException("Unsupported query file format: " + name);
    }

    private QueryConfig loadQueryConfigXml(Path path) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setIgnoringComments(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(path.toFile());
            QueryConfig qc = new QueryConfig();

            Element root = doc.getDocumentElement();

            qc.setExcel(parseExcelConfig(root));
            qc.setVars(parseVars(root));
            qc.setQueryDefs(parseQueryDefs(root));
            qc.setDynamicVars(parseDynamicVars(root));
            qc.setSheets(parseSheets(root, qc.getVars(), qc.getQueryDefs()));
            qc.setDynamicSheets(parseDynamicSheets(root, qc.getVars(), qc.getQueryDefs()));
            applyStyles(qc);

            return qc;
        } catch (Exception e) {
            throw new IOException("Failed to parse XML query file: " + path, e);
        }
    }

    private ExcelConfig parseExcelConfig(Element root) {
        ExcelConfig excel = new ExcelConfig();
        NodeList excelNodes = root.getElementsByTagName("excel");
        if (excelNodes.getLength() > 0) {
            Element el = (Element) excelNodes.item(0);
            excel.setDb(getAttr(el, "db"));
            String output = getAttr(el, "output");
            if (output != null) {
                excel.setOutput(output);
            }
            String maxRows = getAttr(el, "maxRows");
            if (maxRows != null) {
                try {
                    excel.setMaxRows(Integer.parseInt(maxRows));
                } catch (NumberFormatException ignored) {
                }
            }
            excel.setStyle(getAttr(el, "style"));
            excel.setDateColumnFormat(getAttr(el, "date-column-format"));
        }
        return excel;
    }

    private Map<String, Object> parseVars(Element root) {
        Map<String, Object> vars = new LinkedHashMap<>();
        NodeList varList = root.getElementsByTagName("vars");
        if (varList.getLength() == 0) {
            return vars;
        }
        Element varsEl = (Element) varList.item(0);
        NodeList children = varsEl.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && "var".equals(node.getNodeName())) {
                Element var = (Element) node;
                String name = var.getAttribute("name");
                String value = var.getTextContent();
                if (name != null && !name.isEmpty()) {
                    vars.put(name, parseVarValue(value));
                }
            }
        }
        return vars;
    }

    private Object parseVarValue(String value) {
        String trimmed = value.trim();
        if ((trimmed.startsWith("[") && trimmed.endsWith("]")) ||
                (trimmed.startsWith("{") && trimmed.endsWith("}"))) {
            try {
                return objectMapper.readValue(trimmed, Object.class);
            } catch (IOException ignored) {
            }
        }
        return trimmed;
    }

    private Map<String, String> parseQueryDefs(Element root) {
        Map<String, String> defs = new LinkedHashMap<>();
        NodeList defNodes = root.getElementsByTagName("queryDef");
        for (int i = 0; i < defNodes.getLength(); i++) {
            Element el = (Element) defNodes.item(i);
            String id = el.getAttribute("id");
            if (id == null || id.isEmpty()) {
                continue;
            }
            StringBuilder sql = new StringBuilder();
            NodeList children = el.getChildNodes();
            for (int j = 0; j < children.getLength(); j++) {
                Node child = children.item(j);
                if (child.getNodeType() == Node.CDATA_SECTION_NODE || child.getNodeType() == Node.TEXT_NODE) {
                    sql.append(child.getTextContent());
                }
            }
            defs.put(id, sql.toString().trim());
        }
        return defs;
    }

    private List<SheetConfig> parseSheets(Element root, Map<String, Object> vars, Map<String, String> queryDefs) {
        List<SheetConfig> sheets = new ArrayList<>();
        NodeList sheetNodes = root.getElementsByTagName("sheet");
        for (int i = 0; i < sheetNodes.getLength(); i++) {
            Element el = (Element) sheetNodes.item(i);
            SheetConfig sheet = new SheetConfig();
            sheet.setName(getAttr(el, "name"));
            String use = getAttr(el, "use");
            sheet.setUse(!"false".equalsIgnoreCase(use));
            String maxRows = getAttr(el, "maxRows");
            if (maxRows != null) {
                try {
                    sheet.setMaxRows(Integer.parseInt(maxRows));
                } catch (NumberFormatException ignored) {
                }
            }
            sheet.setDb(getAttr(el, "db"));
            sheet.setAggregateColumn(getAttr(el, "aggregateColumn"));
            sheet.setExceptColumns(getAttr(el, "exceptColumns"));
            sheet.setStyle(getAttr(el, "style"));
            sheet.setDateColumnFormat(getAttr(el, "date-column-format"));
            sheet.setQueryRef(getAttr(el, "queryRef"));
            sheet.setLocDesc(getAttr(el, "loc_desc"));
            sheet.setSheetComments(getAttr(el, "sheet_comments"));

            sheet.setParams(parseSheetParams(el));

            if (sheet.getQueryRef() != null && !sheet.getQueryRef().isEmpty()) {
                String query = queryDefs != null ? queryDefs.get(sheet.getQueryRef()) : null;
                if (query == null || query.isEmpty()) {
                    throw new IllegalStateException("queryDef not found: " + sheet.getQueryRef());
                }
                sheet.setQuery(query);
            } else {
                NodeList queryNodes = el.getElementsByTagName("query");
                if (queryNodes.getLength() > 0) {
                    Element queryEl = (Element) queryNodes.item(0);
                    sheet.setQuery(queryEl.getTextContent().trim());
                    sheet.setHiddenColumns(getAttr(queryEl, "hide_columns"));
                } else {
                    StringBuilder sql = new StringBuilder();
                    NodeList children = el.getChildNodes();
                    for (int j = 0; j < children.getLength(); j++) {
                        Node child = children.item(j);
                        if (child.getNodeType() == Node.CDATA_SECTION_NODE || child.getNodeType() == Node.TEXT_NODE) {
                            sql.append(child.getTextContent());
                        }
                    }
                    sheet.setQuery(sql.toString().trim());
                }
            }
            sheets.add(sheet);
        }
        return sheets;
    }

    private List<SheetConfig> parseDynamicSheets(Element root, Map<String, Object> vars, Map<String, String> queryDefs) {
        List<SheetConfig> sheets = new ArrayList<>();
        NodeList sheetNodes = root.getElementsByTagName("dynamic-sheet");
        for (int i = 0; i < sheetNodes.getLength(); i++) {
            Element el = (Element) sheetNodes.item(i);
            SheetConfig sheet = new SheetConfig();
            sheet.setName(getAttr(el, "name"));
            sheet.setIterVar(getAttr(el, "for"));
            String use = getAttr(el, "use");
            sheet.setUse(!"false".equalsIgnoreCase(use));
            String maxRows = getAttr(el, "maxRows");
            if (maxRows != null) {
                try {
                    sheet.setMaxRows(Integer.parseInt(maxRows));
                } catch (NumberFormatException ignored) {
                }
            }
            sheet.setDb(getAttr(el, "db"));
            sheet.setAggregateColumn(getAttr(el, "aggregateColumn"));
            sheet.setExceptColumns(getAttr(el, "exceptColumns"));
            sheet.setStyle(getAttr(el, "style"));
            sheet.setDateColumnFormat(getAttr(el, "date-column-format"));
            sheet.setQueryRef(getAttr(el, "queryRef"));
            sheet.setLocDesc(getAttr(el, "loc_desc"));
            sheet.setSheetComments(getAttr(el, "sheet_comments"));

            sheet.setParams(parseSheetParams(el));

            if (sheet.getQueryRef() != null && !sheet.getQueryRef().isEmpty()) {
                String query = queryDefs != null ? queryDefs.get(sheet.getQueryRef()) : null;
                if (query == null || query.isEmpty()) {
                    throw new IllegalStateException("queryDef not found: " + sheet.getQueryRef());
                }
                sheet.setQuery(query);
            } else {
                NodeList queryNodes = el.getElementsByTagName("query");
                if (queryNodes.getLength() > 0) {
                    Element queryEl = (Element) queryNodes.item(0);
                    sheet.setQuery(queryEl.getTextContent().trim());
                    sheet.setHiddenColumns(getAttr(queryEl, "hide_columns"));
                } else {
                    StringBuilder sql = new StringBuilder();
                    NodeList children = el.getChildNodes();
                    for (int j = 0; j < children.getLength(); j++) {
                        Node child = children.item(j);
                        if (child.getNodeType() == Node.CDATA_SECTION_NODE || child.getNodeType() == Node.TEXT_NODE) {
                            sql.append(child.getTextContent());
                        }
                    }
                    sheet.setQuery(sql.toString().trim());
                }
            }
            sheets.add(sheet);
        }
        return sheets;
    }

    private Map<String, Object> parseSheetParams(Element sheetEl) {
        Map<String, Object> params = new LinkedHashMap<>();
        NodeList paramLists = sheetEl.getElementsByTagName("params");
        if (paramLists.getLength() == 0) {
            return params;
        }
        Element paramsEl = (Element) paramLists.item(0);
        NodeList children = paramsEl.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && "param".equals(node.getNodeName())) {
                Element param = (Element) node;
                String name = param.getAttribute("name");
                String value = param.getTextContent();
                if (name != null && !name.isEmpty()) {
                    params.put(name, parseVarValue(value));
                }
            }
        }
        return params;
    }

    private List<DynamicVarConfig> parseDynamicVars(Element root) {
        List<DynamicVarConfig> vars = new ArrayList<>();
        NodeList nodes = root.getElementsByTagName("dynamicVar");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            DynamicVarConfig dv = new DynamicVarConfig();
            dv.setName(getAttr(el, "name"));
            dv.setType(getAttr(el, "type"));
            dv.setDb(getAttr(el, "db") != null ? getAttr(el, "db") : getAttr(el, "database"));
            StringBuilder sql = new StringBuilder();
            NodeList children = el.getChildNodes();
            for (int j = 0; j < children.getLength(); j++) {
                Node child = children.item(j);
                if (child.getNodeType() == Node.CDATA_SECTION_NODE || child.getNodeType() == Node.TEXT_NODE) {
                    sql.append(child.getTextContent());
                }
            }
            dv.setQuery(sql.toString().trim());
            if (dv.getType() == null || dv.getType().isEmpty()) {
                dv.setType("column_identified");
            }
            vars.add(dv);
        }
        return vars;
    }

    private void applyStyles(QueryConfig qc) {
        if (qc == null) {
            return;
        }
        StyleTemplate.apply(qc.getExcel());
        if (qc.getSheets() != null) {
            for (SheetConfig sheet : qc.getSheets()) {
                StyleTemplate.apply(sheet);
            }
        }
        if (qc.getDynamicSheets() != null) {
            for (SheetConfig sheet : qc.getDynamicSheets()) {
                StyleTemplate.apply(sheet);
            }
        }
    }

    private String getAttr(Element el, String name) {
        if (!el.hasAttribute(name)) {
            return null;
        }
        String value = el.getAttribute(name);
        return value.isEmpty() ? null : value;
    }
}
