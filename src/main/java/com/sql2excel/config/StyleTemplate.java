package com.sql2excel.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class StyleTemplate {

    private static final Map<String, Style> TEMPLATES = new HashMap<>();

    public static void load(Path path) throws IOException {
        TEMPLATES.clear();
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Map<String, Object>> raw = mapper.readValue(path.toFile(),
                new TypeReference<Map<String, Map<String, Object>>>() {});
        for (Map.Entry<String, Map<String, Object>> entry : raw.entrySet()) {
            Map<String, Object> template = entry.getValue();
            @SuppressWarnings("unchecked")
            Map<String, Object> header = (Map<String, Object>) template.get("header");
            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) template.get("body");
            TEMPLATES.put(entry.getKey().toLowerCase(), new Style(header, body));
        }
    }

    public static void apply(ExcelConfig config) {
        if (config == null || config.getStyle() == null || config.getStyle().isEmpty()) {
            return;
        }
        Style style = TEMPLATES.get(config.getStyle().toLowerCase());
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
        Style style = TEMPLATES.get(sheet.getStyle().toLowerCase());
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

    private static class Style {
        final Map<String, Object> header;
        final Map<String, Object> body;

        Style(Map<String, Object> header, Map<String, Object> body) {
            this.header = header != null ? header : Collections.emptyMap();
            this.body = body != null ? body : Collections.emptyMap();
        }
    }
}
