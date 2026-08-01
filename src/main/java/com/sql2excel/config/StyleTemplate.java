package com.sql2excel.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StyleTemplate {

    private static final Map<String, Style> TEMPLATES = new LinkedHashMap<>();

    public static void load(Path path) throws IOException {
        TEMPLATES.clear();
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Map<String, Object>> raw = mapper.readValue(path.toFile(),
                new TypeReference<Map<String, Map<String, Object>>>() {});
        Map<String, Style> temp = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Object>> entry : raw.entrySet()) {
            String name = entry.getKey().toLowerCase();
            Map<String, Object> template = entry.getValue();
            Map<String, Object> header = getMap(template, "header");
            Map<String, Object> body = getMap(template, "body");
            temp.put(name, new Style(header, body));
        }
        Style defaultStyle = temp.get("default");
        for (Map.Entry<String, Style> entry : temp.entrySet()) {
            Style base = "default".equals(entry.getKey()) ? null : defaultStyle;
            Style style = entry.getValue();
            Map<String, Object> baseHeader = base != null ? base.header : null;
            Map<String, Object> baseBody = base != null ? base.body : null;
            TEMPLATES.put(entry.getKey(), new Style(
                    processHeader(style.header, baseHeader),
                    processBody(style.body, baseBody)
            ));
        }
    }

    public static void apply(ExcelConfig config) {
        if (config == null || config.getStyle() == null || config.getStyle().isEmpty()) {
            return;
        }
        Style style = resolveStyle(config.getStyle());
        if (style == null) {
            return;
        }
        if (config.getHeader() == null && !style.header.isEmpty()) {
            config.setHeader(new HashMap<>(style.header));
        }
        if (config.getBody() == null && !style.body.isEmpty()) {
            config.setBody(new HashMap<>(style.body));
        }
    }

    public static void apply(SheetConfig sheet) {
        if (sheet == null || sheet.getStyle() == null || sheet.getStyle().isEmpty()) {
            return;
        }
        Style style = resolveStyle(sheet.getStyle());
        if (style == null) {
            return;
        }
        if (sheet.getHeader() == null && !style.header.isEmpty()) {
            sheet.setHeader(new HashMap<>(style.header));
        }
        if (sheet.getBody() == null && !style.body.isEmpty()) {
            sheet.setBody(new HashMap<>(style.body));
        }
    }

    public static List<String> getStyleNames() {
        return new ArrayList<>(TEMPLATES.keySet());
    }

    public static Map<String, Object> getStyleHeader(String name) {
        Style style = TEMPLATES.get(name != null ? name.toLowerCase() : "");
        return style != null ? style.header : null;
    }

    public static Map<String, Object> getStyleBody(String name) {
        Style style = resolveStyle(name);
        return style != null ? style.body : null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getMap(Map<String, Object> parent, String key) {
        Object value = parent.get(key);
        return value instanceof Map ? (Map<String, Object>) value : null;
    }

    private static Map<String, Object> processHeader(Map<String, Object> header, Map<String, Object> base) {
        if (header != null && header.isEmpty()) {
            return header; // explicit empty header means no header style
        }
        Map<String, Object> merged = deepMerge(base, header);
        if (merged == null || merged.isEmpty()) {
            return merged;
        }
        merged.put("alignment", completeAlignment(merged.get("alignment"), "center"));
        return merged;
    }

    private static Map<String, Object> processBody(Map<String, Object> body, Map<String, Object> base) {
        if (body != null && body.isEmpty()) {
            return body; // explicit empty body means no body style
        }
        Map<String, Object> merged = deepMerge(base, body);
        if (merged == null || merged.isEmpty()) {
            return merged;
        }
        if (isNestedBody(merged)) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("default", applyTypeDefaults(getMap(merged, "default"), "left"));
            result.put("number", applyTypeDefaults(getMap(merged, "number"), "right"));
            result.put("date", applyTypeDefaults(getMap(merged, "date"), "center"));
            return result;
        }
        // flat body: treat the entire map as the default style
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("default", applyTypeDefaults(merged, "left"));
        return result;
    }

    private static Map<String, Object> deepMerge(Map<String, Object> base, Map<String, Object> override) {
        if (base == null) {
            return override == null ? null : copyMap(override);
        }
        if (override == null) {
            return copyMap(base);
        }
        if (override.isEmpty()) {
            return new HashMap<>(); // explicit empty map overrides with an empty map
        }
        Map<String, Object> result = new HashMap<>(base);
        for (Map.Entry<String, Object> entry : override.entrySet()) {
            Object baseValue = result.get(entry.getKey());
            Object overrideValue = entry.getValue();
            if (baseValue instanceof Map && overrideValue instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> merged = deepMerge((Map<String, Object>) baseValue, (Map<String, Object>) overrideValue);
                result.put(entry.getKey(), merged);
            } else {
                result.put(entry.getKey(), overrideValue);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> copyMap(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        Map<String, Object> copy = new HashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map) {
                copy.put(entry.getKey(), copyMap((Map<String, Object>) value));
            } else {
                copy.put(entry.getKey(), value);
            }
        }
        return copy;
    }

    private static boolean isNestedBody(Map<String, Object> body) {
        Object defaultObj = body.get("default");
        return defaultObj instanceof Map;
    }

    private static Map<String, Object> applyTypeDefaults(Map<String, Object> subMap, String defaultHorizontal) {
        Map<String, Object> result = subMap != null ? new HashMap<>(subMap) : new HashMap<>();
        result.put("alignment", completeAlignment(result.get("alignment"), defaultHorizontal));
        return result;
    }

    private static Map<String, Object> completeAlignment(Object alignObj, String defaultHorizontal) {
        Map<String, Object> alignment = new HashMap<>();
        if (alignObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> existing = (Map<String, Object>) alignObj;
            alignment.putAll(existing);
        }
        if (!alignment.containsKey("horizontal")) {
            alignment.put("horizontal", defaultHorizontal);
        }
        if (!alignment.containsKey("vertical")) {
            alignment.put("vertical", "middle");
        }
        return alignment;
    }

    private static Style resolveStyle(String name) {
        if (name == null || name.isEmpty()) {
            return TEMPLATES.get("default");
        }
        String key = name.toLowerCase();
        Style style = TEMPLATES.get(key);
        if (style == null && !"default".equals(key)) {
            style = TEMPLATES.get("default");
        }
        return style;
    }

    private static class Style {
        final Map<String, Object> header;
        final Map<String, Object> body;

        Style(Map<String, Object> header, Map<String, Object> body) {
            this.header = header != null ? header : Collections.emptyMap();
            this.body = body != null ? body : Collections.emptyMap();
        }
    }
}
