package com.sql2excel.config;

import org.w3c.dom.*;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Strict structural validator for XML query definition files.
 * This validator checks element/attribute names, required attributes,
 * duplicate names/ids, queryRef targets, and variable references.
 */
public class XmlQueryValidator {

    private static final Set<String> EXCEL_ATTRS = new HashSet<>(Arrays.asList(
            "db", "output", "maxRows", "style", "date-column-format", "aggregateInfoTemplate"
    ));

    private static final Set<String> SHEET_ATTRS = new HashSet<>(Arrays.asList(
            "name", "use", "maxRows", "db", "aggregateColumn", "exceptColumns",
            "style", "date-column-format", "queryRef", "aggregateInfoTemplate", "hiddenColumns"
    ));

    private static final Set<String> DYNAMIC_SHEET_ATTRS = new HashSet<>(Arrays.asList(
            "name", "for", "use", "maxRows", "db", "aggregateColumn", "exceptColumns",
            "style", "date-column-format", "queryRef", "aggregateInfoTemplate", "hiddenColumns"
    ));

    private static final Set<String> DYNAMIC_VAR_ATTRS = new HashSet<>(Arrays.asList(
            "name", "type", "db", "database", "description"
    ));

    private static final Set<String> QUERY_DEF_ATTRS = new HashSet<>(Arrays.asList(
            "id", "description"
    ));

    private static final Set<String> VAR_ATTRS = new HashSet<>(Collections.singletonList("name"));

    private static final Set<String> QUERY_ATTRS = new HashSet<>(Collections.singletonList("hide_columns"));

    private static final Set<String> PARAM_ATTRS = new HashSet<>(Collections.singletonList("name"));

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}]+)\\}");

    public static class ValidationResult {
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();

        public void addError(String message) {
            errors.add(message);
        }

        public void addError(Element el, String message) {
            errors.add(prefixLine(el, message));
        }

        public void addWarning(String message) {
            warnings.add(message);
        }

        public void addWarning(Element el, String message) {
            warnings.add(prefixLine(el, message));
        }

        private String prefixLine(Element el, String message) {
            if (el == null) {
                return message;
            }
            Object line = el.getUserData("line");
            if (line != null) {
                return "[line " + line + "] " + message;
            }
            return message;
        }

        public List<String> getErrors() {
            List<String> sorted = new ArrayList<>(errors);
            sorted.sort(XmlQueryValidator::byLine);
            return sorted;
        }

        public List<String> getWarnings() {
            List<String> sorted = new ArrayList<>(warnings);
            sorted.sort(XmlQueryValidator::byLine);
            return sorted;
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }
    }

    public static ValidationResult validate(Path file, Map<String, DatabaseConfig> databases) throws IOException {
        ValidationResult result = new ValidationResult();
        try (InputStream in = Files.newInputStream(file)) {
            SaxDomBuilder builder = new SaxDomBuilder();
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(false);
            SAXParser parser = factory.newSAXParser();
            parser.parse(new InputSource(in), builder);
            Document doc = builder.getDocument();
            validateDocument(doc, databases, result);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            result.addError("Failed to parse XML query file: " + e.getMessage());
        }
        return result;
    }

    private static void validateDocument(Document doc, Map<String, DatabaseConfig> databases, ValidationResult result) {
        Element root = doc.getDocumentElement();
        if (root == null) {
            result.addError("XML document has no root element");
            return;
        }
        if (!"queries".equals(root.getNodeName())) {
            result.addError(root, "Root element must be <queries>, found <" + root.getNodeName() + ">");
            return;
        }

        List<Element> topLevel = getChildElements(root);
        if (topLevel.isEmpty()) {
            result.addError(root, "No top-level elements found under <queries>");
            return;
        }

        List<Element> excelElements = new ArrayList<>();
        List<Element> sheetElements = new ArrayList<>();
        List<Element> dynamicSheetElements = new ArrayList<>();
        List<Element> varElements = new ArrayList<>();
        List<Element> queryDefElements = new ArrayList<>();
        List<Element> dynamicVarElements = new ArrayList<>();

        for (Element el : topLevel) {
            String name = el.getNodeName();
            switch (name) {
                case "excel":
                    excelElements.add(el);
                    break;
                case "sheet":
                    sheetElements.add(el);
                    break;
                case "sheets":
                    if (hasAnyAttribute(el)) {
                        result.addError(el, "<sheets> wrapper must not have attributes");
                    }
                    for (Element child : getChildElements(el)) {
                        if (!"sheet".equals(child.getNodeName())) {
                            result.addError(child, "<sheets> may only contain <sheet> elements, found <" + child.getNodeName() + ">");
                        } else {
                            sheetElements.add(child);
                        }
                    }
                    break;
                case "dynamic-sheet":
                    dynamicSheetElements.add(el);
                    break;
                case "dynamic-sheets":
                    if (hasAnyAttribute(el)) {
                        result.addError(el, "<dynamic-sheets> wrapper must not have attributes");
                    }
                    for (Element child : getChildElements(el)) {
                        if (!"dynamic-sheet".equals(child.getNodeName())) {
                            result.addError(child, "<dynamic-sheets> may only contain <dynamic-sheet> elements, found <" + child.getNodeName() + ">");
                        } else {
                            dynamicSheetElements.add(child);
                        }
                    }
                    break;
                case "vars":
                    varElements.addAll(extractVarElements(el, result));
                    break;
                case "var":
                    // Variables must be wrapped in <vars>; a lone top-level <var> is not a valid variable block.
                    result.addError(el, "Variables must be defined inside a <vars> block, not as top-level <var>");
                    break;
                case "queryDef":
                    queryDefElements.add(el);
                    break;
                case "queryDefs":
                    if (hasAnyAttribute(el)) {
                        result.addError(el, "<queryDefs> wrapper must not have attributes");
                    }
                    for (Element child : getChildElements(el)) {
                        if (!"queryDef".equals(child.getNodeName())) {
                            result.addError(child, "<queryDefs> may only contain <queryDef> elements, found <" + child.getNodeName() + ">");
                        } else {
                            queryDefElements.add(child);
                        }
                    }
                    break;
                case "dynamicVar":
                    dynamicVarElements.add(el);
                    break;
                case "dynamicVars":
                    if (hasAnyAttribute(el)) {
                        result.addError(el, "<dynamicVars> wrapper must not have attributes");
                    }
                    for (Element child : getChildElements(el)) {
                        if (!"dynamicVar".equals(child.getNodeName())) {
                            result.addError(child, "<dynamicVars> may only contain <dynamicVar> elements, found <" + child.getNodeName() + ">");
                        } else {
                            dynamicVarElements.add(child);
                        }
                    }
                    break;
                default:
                    result.addError(el, "Unknown top-level element <" + name + ">");
            }
        }

        if (excelElements.isEmpty()) {
            result.addError(root, "Missing <excel> element");
        } else if (excelElements.size() > 1) {
            result.addError(excelElements.get(0), "Only one <excel> element is allowed, found " + excelElements.size());
        }

        ExcelConfig excelConfig = null;
        if (!excelElements.isEmpty()) {
            excelConfig = validateExcel(excelElements.get(0), databases, result);
        }

        Set<String> varNames = validateVars(varElements, result);
        Set<String> dynamicVarNames = validateDynamicVars(dynamicVarElements, databases, result);
        Set<String> queryDefIds = validateQueryDefs(queryDefElements, result);

        Set<String> allVarNames = new HashSet<>(varNames);
        allVarNames.addAll(dynamicVarNames);

        List<SheetValidation> sheetValidations = new ArrayList<>();
        int activeSheetCount = 0;
        Set<String> seenSheetNames = new HashSet<>();

        for (Element el : sheetElements) {
            SheetValidation sv = validateSheet(el, excelConfig, databases, queryDefIds, result, false);
            sheetValidations.add(sv);
            if (sv.active) {
                activeSheetCount++;
                if (sv.name != null) {
                    if (seenSheetNames.contains(sv.name)) {
                        result.addError(sv.element, "Duplicate sheet name: " + sv.name);
                    } else {
                        seenSheetNames.add(sv.name);
                    }
                }
            }
        }

        for (Element el : dynamicSheetElements) {
            SheetValidation sv = validateSheet(el, excelConfig, databases, queryDefIds, result, true);
            sheetValidations.add(sv);
            if (sv.active) {
                activeSheetCount++;
                if (sv.name != null) {
                    if (seenSheetNames.contains(sv.name)) {
                        result.addError(sv.element, "Duplicate sheet name: " + sv.name);
                    } else {
                        seenSheetNames.add(sv.name);
                    }
                }
            }
        }

        if (activeSheetCount == 0) {
            result.addError(root, "No active sheets to export");
        }

        // Variable reference checks in all relevant texts
        if (excelConfig != null && excelConfig.getOutput() != null) {
            checkPlaceholders(excelElements.get(0), "excel output", excelConfig.getOutput(), allVarNames, result);
        }

        for (SheetValidation sv : sheetValidations) {
            Set<String> localVars = new HashSet<>(allVarNames);
            if (sv.params != null) {
                localVars.addAll(sv.params);
            }
            if (sv.name != null) {
                checkPlaceholders(sv.element, "sheet name", sv.name, localVars, result);
            }
            if (sv.query != null) {
                checkPlaceholders(sv.element, "sheet '" + sv.name + "' query", sv.query, localVars, result);
            }
        }

        for (Element el : queryDefElements) {
            String text = getTextContent(el);
            checkPlaceholders(el, "queryDef '" + getAttr(el, "id") + "'", text, allVarNames, result);
        }

        for (Element el : dynamicVarElements) {
            String text = getTextContent(el);
            checkPlaceholders(el, "dynamicVar '" + getAttr(el, "name") + "' query", text, allVarNames, result);
        }
    }

    private static List<Element> extractVarElements(Element varsEl, ValidationResult result) {
        if (hasAnyAttribute(varsEl)) {
            result.addError(varsEl, "<vars> must not have attributes");
        }
        List<Element> varElements = new ArrayList<>();
        for (Element child : getChildElements(varsEl)) {
            if (!"var".equals(child.getNodeName())) {
                result.addError(child, "<vars> may only contain <var> elements, found <" + child.getNodeName() + ">");
            } else {
                varElements.add(child);
            }
        }
        return varElements;
    }

    private static ExcelConfig validateExcel(Element el, Map<String, DatabaseConfig> databases, ValidationResult result) {
        checkAttributes(el, EXCEL_ATTRS, "excel", result);
        String db = getAttr(el, "db");
        String output = getAttr(el, "output");

        if (db == null || db.isEmpty()) {
            result.addError(el, "<excel> requires 'db' attribute");
        } else if (databases != null && !databases.containsKey(db)) {
            result.addError(el, "<excel> references unknown database: " + db);
        }

        if (output == null || output.isEmpty()) {
            result.addError(el, "<excel> requires 'output' attribute");
        }

        String maxRows = getAttr(el, "maxRows");
        if (maxRows != null && !maxRows.isEmpty()) {
            try {
                int n = Integer.parseInt(maxRows);
                if (n <= 0) {
                    result.addError(el, "<excel> 'maxRows' must be a positive integer");
                }
            } catch (NumberFormatException e) {
                result.addError(el, "<excel> 'maxRows' is not a valid integer: " + maxRows);
            }
        }

        for (Element child : getChildElements(el)) {
            result.addError(child, "<excel> must not contain child elements, found <" + child.getNodeName() + ">");
        }

        ExcelConfig ec = new ExcelConfig();
        ec.setDb(db);
        ec.setOutput(output);
        return ec;
    }

    private static Set<String> validateVars(List<Element> varElements, ValidationResult result) {
        Set<String> names = new HashSet<>();
        for (Element el : varElements) {
            checkAttributes(el, VAR_ATTRS, "var", result);
            String name = getAttr(el, "name");
            if (name == null || name.isEmpty()) {
                result.addError(el, "<var> requires 'name' attribute");
                continue;
            }
            if (names.contains(name)) {
                result.addError(el, "Duplicate variable name: " + name);
            } else {
                names.add(name);
            }
            for (Element child : getChildElements(el)) {
                result.addError(child, "<var> '" + name + "' must not contain child elements, found <" + child.getNodeName() + ">");
            }
        }
        return names;
    }

    private static Set<String> validateDynamicVars(List<Element> dynamicVarElements, Map<String, DatabaseConfig> databases, ValidationResult result) {
        Set<String> names = new HashSet<>();
        for (Element el : dynamicVarElements) {
            checkAttributes(el, DYNAMIC_VAR_ATTRS, "dynamicVar", result);
            String name = getAttr(el, "name");
            if (name == null || name.isEmpty()) {
                result.addError(el, "<dynamicVar> requires 'name' attribute");
                continue;
            }
            if (names.contains(name)) {
                result.addError(el, "Duplicate dynamic variable name: " + name);
            } else {
                names.add(name);
            }

            String db = getAttr(el, "db");
            if (db == null || db.isEmpty()) {
                db = getAttr(el, "database");
            }
            if (db != null && !db.isEmpty() && databases != null && !databases.containsKey(db)) {
                result.addError(el, "<dynamicVar '" + name + "'> references unknown database: " + db);
            }

            boolean dynamicVarHasChild = !getChildElements(el).isEmpty();
            for (Element child : getChildElements(el)) {
                result.addError(child, "<dynamicVar '" + name + "'> must not contain child elements, found <" + child.getNodeName() + ">");
            }

            if (!dynamicVarHasChild && getTextContent(el).isEmpty()) {
                result.addError(el, "<dynamicVar '" + name + "'> has no query");
            }
        }
        return names;
    }

    private static Set<String> validateQueryDefs(List<Element> queryDefElements, ValidationResult result) {
        Set<String> ids = new HashSet<>();
        for (Element el : queryDefElements) {
            checkAttributes(el, QUERY_DEF_ATTRS, "queryDef", result);
            String id = getAttr(el, "id");
            if (id == null || id.isEmpty()) {
                result.addError(el, "<queryDef> requires 'id' attribute");
                continue;
            }
            if (ids.contains(id)) {
                result.addError(el, "Duplicate queryDef id: " + id);
            } else {
                ids.add(id);
            }
            boolean queryDefHasChild = !getChildElements(el).isEmpty();
            for (Element child : getChildElements(el)) {
                result.addError(child, "<queryDef '" + id + "'> must not contain child elements, found <" + child.getNodeName() + ">");
            }

            if (!queryDefHasChild && getTextContent(el).isEmpty()) {
                result.addError(el, "<queryDef '" + id + "'> has no query");
            }
        }
        return ids;
    }

    private static SheetValidation validateSheet(Element el, ExcelConfig excelConfig,
                                                 Map<String, DatabaseConfig> databases,
                                                 Set<String> queryDefIds,
                                                 ValidationResult result,
                                                 boolean dynamic) {
        String tag = dynamic ? "dynamic-sheet" : "sheet";
        Set<String> allowedAttrs = dynamic ? DYNAMIC_SHEET_ATTRS : SHEET_ATTRS;
        checkAttributes(el, allowedAttrs, tag, result);

        SheetValidation sv = new SheetValidation();
        sv.element = el;
        sv.name = getAttr(el, "name");
        String use = getAttr(el, "use");

        if (use != null && !use.isEmpty()) {
            if (!"true".equalsIgnoreCase(use) && !"false".equalsIgnoreCase(use)) {
                result.addError(el, "<" + tag + " '" + sv.name + "'> 'use' must be 'true' or 'false', found: " + use);
                sv.active = false;
            } else {
                sv.active = Boolean.parseBoolean(use);
            }
        } else {
            sv.active = true;
        }

        if (sv.name == null || sv.name.isEmpty()) {
            result.addError(el, "<" + tag + "> requires 'name' attribute");
        } else if (sv.name.length() > 31) {
            result.addWarning(el, "Sheet name '" + sv.name + "' exceeds 31 characters");
        }

        String db = getAttr(el, "db");
        String effectiveDb = db != null && !db.isEmpty() ? db : (excelConfig != null ? excelConfig.getDb() : null);
        if (effectiveDb == null || effectiveDb.isEmpty()) {
            result.addError(el, "<" + tag + " '" + sv.name + "'> has no database defined");
        } else if (databases != null && !databases.containsKey(effectiveDb)) {
            result.addError(el, "<" + tag + " '" + sv.name + "'> references unknown database: " + effectiveDb);
        }

        String maxRows = getAttr(el, "maxRows");
        if (maxRows != null && !maxRows.isEmpty()) {
            try {
                int n = Integer.parseInt(maxRows);
                if (n <= 0) {
                    result.addError(el, "<" + tag + " '" + sv.name + "'> 'maxRows' must be a positive integer");
                }
            } catch (NumberFormatException e) {
                result.addError(el, "<" + tag + " '" + sv.name + "'> 'maxRows' is not a valid integer: " + maxRows);
            }
        }

        if (dynamic) {
            String forAttr = getAttr(el, "for");
            if (forAttr == null || forAttr.isEmpty()) {
                result.addError(el, "<dynamic-sheet '" + sv.name + "'> requires 'for' attribute");
            }
        }

        // Extract query text, queryRef, and params
        String queryRef = getAttr(el, "queryRef");
        sv.query = null;
        boolean queryHasChildError = false;
        List<Element> queryChildren = getChildElementsByTagName(el, "query");
        List<Element> paramsChildren = getChildElementsByTagName(el, "params");

        for (Element child : getChildElements(el)) {
            String childName = child.getNodeName();
            if (!"query".equals(childName) && !"params".equals(childName)) {
                result.addError(child, "<" + tag + " '" + sv.name + "'> contains unknown child element <" + childName + ">");
            }
        }

        if (queryRef != null && !queryRef.isEmpty()) {
            if (!queryDefIds.contains(queryRef)) {
                result.addError(el, "<" + tag + " '" + sv.name + "'> references unknown queryDef: " + queryRef);
            }
            sv.query = null; // query is fetched from queryDef, not sheet body
        } else if (!queryChildren.isEmpty()) {
            Element queryEl = queryChildren.get(0);
            checkAttributes(queryEl, QUERY_ATTRS, "query", result);
            for (Element qChild : getChildElements(queryEl)) {
                result.addError(qChild, "<" + tag + " '" + sv.name + "'> <query> must not contain child elements, found <" + qChild.getNodeName() + ">");
            }
            queryHasChildError = !getChildElements(queryEl).isEmpty();
            sv.query = getTextContent(queryEl);
            if (queryChildren.size() > 1) {
                result.addError(queryEl, "<" + tag + " '" + sv.name + "'> contains more than one <query>");
            }
        } else {
            sv.query = getTextContent(el);
        }

        if (sv.active && (sv.query == null || sv.query.isEmpty()) && (queryRef == null || queryRef.isEmpty()) && !queryHasChildError) {
            result.addError(el, "<" + tag + " '" + sv.name + "'> has no query or queryRef");
        }

        // Parse sheet-level params
        if (!paramsChildren.isEmpty()) {
            Element paramsEl = paramsChildren.get(0);
            if (hasAnyAttribute(paramsEl)) {
                result.addError(paramsEl, "<" + tag + " '" + sv.name + "'> <params> must not have attributes");
            }
            sv.params = new HashSet<>();
            for (Element param : getChildElements(paramsEl)) {
                if (!"param".equals(param.getNodeName())) {
                    result.addError(param, "<params> may only contain <param>, found <" + param.getNodeName() + ">");
                    continue;
                }
                checkAttributes(param, PARAM_ATTRS, "param", result);
                String paramName = getAttr(param, "name");
                if (paramName == null || paramName.isEmpty()) {
                    result.addError(param, "<param> in sheet '" + sv.name + "' requires 'name' attribute");
                } else {
                    sv.params.add(paramName);
                }
                for (Element pChild : getChildElements(param)) {
                    result.addError(pChild, "<param> '" + paramName + "' must not contain child elements, found <" + pChild.getNodeName() + ">");
                }
            }
        }

        return sv;
    }

    private static void checkPlaceholders(Element source, String context, String text, Set<String> knownVars, ValidationResult result) {
        if (text == null || text.isEmpty()) {
            return;
        }
        Matcher matcher = PLACEHOLDER.matcher(text);
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            String key = placeholder;
            int colonIndex = placeholder.indexOf(':');
            if (colonIndex >= 0) {
                key = placeholder.substring(0, colonIndex).trim();
            }

            if (isDateVariable(key) || isSpecialVariable(key)) {
                continue;
            }

            // Allow dynamic var column references like dvName.column
            if (hasDynamicVarPrefix(key, knownVars)) {
                continue;
            }

            if (!knownVars.contains(key)) {
                result.addWarning(source, "Unresolved variable reference in " + context + ": ${" + placeholder + "}");
            }
        }
    }

    private static boolean isDateVariable(String key) {
        return key != null && (key.equalsIgnoreCase("DATE") || key.toLowerCase().startsWith("date."));
    }

    private static boolean isSpecialVariable(String key) {
        if (key == null) {
            return false;
        }
        String lower = key.toLowerCase();
        return "current_timestamp".equals(lower)
                || "current_date".equals(lower)
                || "current_time".equals(lower)
                || "unix_timestamp".equals(lower)
                || "today".equals(lower)
                || "getdate".equals(lower)
                || "getdate()".equals(lower);
    }

    private static boolean hasDynamicVarPrefix(String key, Set<String> dynamicVarNames) {
        if (key == null) {
            return false;
        }
        int dotIndex = key.indexOf('.');
        if (dotIndex < 0) {
            return false;
        }
        String prefix = key.substring(0, dotIndex);
        return dynamicVarNames.contains(prefix);
    }

    private static void checkAttributes(Element el, Set<String> allowed, String tagName, ValidationResult result) {
        NamedNodeMap attrs = el.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Node attr = attrs.item(i);
            String name = attr.getNodeName();
            if (!allowed.contains(name)) {
                result.addError(el, "<" + tagName + "> has unknown attribute '" + name + "'");
            }
        }
    }

    private static boolean hasAnyAttribute(Element el) {
        return el.getAttributes().getLength() > 0;
    }

    private static boolean hasChildElements(Element el) {
        return !getChildElements(el).isEmpty();
    }

    private static List<Element> getChildElements(Element parent) {
        List<Element> result = new ArrayList<>();
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                result.add((Element) n);
            }
        }
        return result;
    }

    private static List<Element> getChildElementsByTagName(Element parent, String tagName) {
        List<Element> result = new ArrayList<>();
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE && tagName.equals(n.getNodeName())) {
                result.add((Element) n);
            }
        }
        return result;
    }

    private static String getTextContent(Element el) {
        StringBuilder sb = new StringBuilder();
        NodeList children = el.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n.getNodeType() == Node.CDATA_SECTION_NODE || n.getNodeType() == Node.TEXT_NODE) {
                sb.append(n.getTextContent());
            }
        }
        return sb.toString().trim();
    }

    private static String getAttr(Element el, String name) {
        if (!el.hasAttribute(name)) {
            return null;
        }
        String value = el.getAttribute(name);
        return value.isEmpty() ? null : value;
    }

    private static int byLine(String a, String b) {
        int lineA = extractLine(a);
        int lineB = extractLine(b);
        if (lineA != lineB) {
            return Integer.compare(lineA, lineB);
        }
        return a.compareTo(b);
    }

    private static int extractLine(String s) {
        if (s == null || !s.startsWith("[line ")) {
            return Integer.MAX_VALUE;
        }
        int end = s.indexOf(']');
        if (end < 0) {
            return Integer.MAX_VALUE;
        }
        try {
            return Integer.parseInt(s.substring(6, end));
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    private static class SheetValidation {
        Element element;
        String name;
        boolean active;
        String query;
        Set<String> params;
    }

    private static class SaxDomBuilder extends DefaultHandler implements LexicalHandler {
        private Document doc;
        private Element root;
        private Element current;
        private Locator locator;
        private boolean inCdata;

        public Document getDocument() {
            return doc;
        }

        @Override
        public void startDocument() throws SAXException {
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(false);
                DocumentBuilder builder = factory.newDocumentBuilder();
                doc = builder.newDocument();
            } catch (Exception e) {
                throw new SAXException("Failed to create document", e);
            }
        }

        @Override
        public void endDocument() throws SAXException {
            if (doc != null && root != null) {
                doc.appendChild(root);
            }
        }

        @Override
        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            Element el = doc.createElement(qName);
            for (int i = 0; i < attributes.getLength(); i++) {
                String attrQName = attributes.getQName(i);
                if (attrQName != null && !attrQName.isEmpty()) {
                    el.setAttribute(attrQName, attributes.getValue(i));
                } else if (localName != null && !localName.isEmpty()) {
                    el.setAttribute(localName, attributes.getValue(i));
                }
            }
            if (locator != null) {
                el.setUserData("line", locator.getLineNumber(), null);
            }
            if (current == null) {
                root = el;
            } else {
                current.appendChild(el);
            }
            current = el;
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (current != null) {
                current = (Element) current.getParentNode();
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (current == null || length == 0) {
                return;
            }
            String text = new String(ch, start, length);
            if (inCdata) {
                current.appendChild(doc.createCDATASection(text));
            } else {
                current.appendChild(doc.createTextNode(text));
            }
        }

        @Override
        public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
            characters(ch, start, length);
        }

        @Override
        public void startCDATA() throws SAXException {
            inCdata = true;
        }

        @Override
        public void endCDATA() throws SAXException {
            inCdata = false;
        }

        @Override
        public void startDTD(String name, String publicId, String systemId) throws SAXException {
            // ignore
        }

        @Override
        public void endDTD() throws SAXException {
            // ignore
        }

        @Override
        public void startEntity(String name) throws SAXException {
            // ignore
        }

        @Override
        public void endEntity(String name) throws SAXException {
            // ignore
        }

        @Override
        public void comment(char[] ch, int start, int length) throws SAXException {
            // ignore
        }
    }
}
