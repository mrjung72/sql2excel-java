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
        String name = path.getFileName().toString().toLowerCase();
        if (name.endsWith(".json")) {
            return objectMapper.readValue(path.toFile(), QueryConfig.class);
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
            qc.setSheets(parseSheets(root, qc.getVars()));

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

    private List<SheetConfig> parseSheets(Element root, Map<String, Object> vars) {
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

            StringBuilder sql = new StringBuilder();
            NodeList children = el.getChildNodes();
            for (int j = 0; j < children.getLength(); j++) {
                Node child = children.item(j);
                if (child.getNodeType() == Node.CDATA_SECTION_NODE || child.getNodeType() == Node.TEXT_NODE) {
                    sql.append(child.getTextContent());
                }
            }
            sheet.setQuery(sql.toString().trim());
            sheets.add(sheet);
        }
        return sheets;
    }

    private String getAttr(Element el, String name) {
        if (!el.hasAttribute(name)) {
            return null;
        }
        String value = el.getAttribute(name);
        return value.isEmpty() ? null : value;
    }
}
