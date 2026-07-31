package com.sql2excel.variable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VariableResolver {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}]+)\\}");

    private static final Map<String, Integer> TIMEZONE_OFFSETS = new LinkedHashMap<>();

    static {
        TIMEZONE_OFFSETS.put("UTC", 0);
        TIMEZONE_OFFSETS.put("GMT", 0);
        TIMEZONE_OFFSETS.put("KST", 540);
        TIMEZONE_OFFSETS.put("JST", 540);
        TIMEZONE_OFFSETS.put("CST", 480);
        TIMEZONE_OFFSETS.put("SGT", 480);
        TIMEZONE_OFFSETS.put("PHT", 480);
        TIMEZONE_OFFSETS.put("AEST", 600);
        TIMEZONE_OFFSETS.put("ICT", 420);
        TIMEZONE_OFFSETS.put("CET", 60);
        TIMEZONE_OFFSETS.put("EET", 120);
        TIMEZONE_OFFSETS.put("IST", 330);
        TIMEZONE_OFFSETS.put("GST", 240);
        TIMEZONE_OFFSETS.put("EST", -300);
        TIMEZONE_OFFSETS.put("CST_US", -360);
        TIMEZONE_OFFSETS.put("MST", -420);
        TIMEZONE_OFFSETS.put("PST", -480);
        TIMEZONE_OFFSETS.put("AST", -240);
        TIMEZONE_OFFSETS.put("AKST", -540);
        TIMEZONE_OFFSETS.put("HST", -600);
        TIMEZONE_OFFSETS.put("BRT", -180);
        TIMEZONE_OFFSETS.put("ART", -180);
    }

    private static final List<String[]> TOKEN_MAP = Arrays.asList(
            new String[]{"YYYY", "yyyy"},
            new String[]{"yyyy", "yyyy"},
            new String[]{"YY", "yy"},
            new String[]{"yy", "yy"},
            new String[]{"SSS", "SSS"},
            new String[]{"sss", "SSS"},
            new String[]{"HH", "HH"},
            new String[]{"hh", "HH"},
            new String[]{"MM", "MM"},
            new String[]{"DD", "dd"},
            new String[]{"dd", "dd"},
            new String[]{"D", "d"},
            new String[]{"d", "d"},
            new String[]{"H", "H"},
            new String[]{"h", "H"},
            new String[]{"M", "M"},
            new String[]{"mm", "mm"},
            new String[]{"ss", "ss"},
            new String[]{"m", "m"},
            new String[]{"s", "s"}
    );

    public String resolve(String text, Map<String, Object> vars) {
        return resolve(text, vars, false);
    }

    public String resolveSql(String sql, Map<String, Object> vars) {
        return resolve(sql, vars, true);
    }

    private String resolve(String text, Map<String, Object> vars, boolean sqlContext) {
        if (text == null) {
            return null;
        }
        Map<String, Object> ciVars = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (vars != null) {
            ciVars.putAll(vars);
        }
        StringBuffer result = new StringBuffer();
        Matcher matcher = PLACEHOLDER.matcher(text);
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            String replacement = resolvePlaceholder(placeholder, ciVars, sqlContext);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private String resolvePlaceholder(String placeholder, Map<String, Object> vars, boolean sqlContext) {
        String key = placeholder;
        String format = null;
        int colonIndex = placeholder.indexOf(':');
        if (colonIndex >= 0) {
            key = placeholder.substring(0, colonIndex).trim();
            format = placeholder.substring(colonIndex + 1).trim();
        }

        if (key.length() >= 4 && key.substring(0, 4).equalsIgnoreCase("DATE")) {
            return formatDate(key, format);
        }

        String special = resolveSpecialVariables(key, format);
        if (special != null) {
            return special;
        }

        Object value = vars.get(key);
        if (value == null) {
            return "${" + placeholder + "}";
        }

        if (value instanceof List) {
            List<?> list = (List<?>) value;
            if (sqlContext) {
                return createInClause(list);
            } else {
                return String.join(",", listToStrings(list));
            }
        }

        return value.toString();
    }

    private String createInClause(List<?> values) {
        if (values.isEmpty()) {
            return "NULL";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            Object v = values.get(i);
            if (v instanceof Number) {
                sb.append(v);
            } else {
                sb.append("'").append(v.toString().replace("'", "''")).append("'");
            }
        }
        return sb.toString();
    }

    private List<String> listToStrings(List<?> list) {
        List<String> result = new ArrayList<>();
        for (Object v : list) {
            result.add(v.toString());
        }
        return result;
    }

    private String resolveSpecialVariables(String key, String format) {
        String pattern = (format == null || format.isEmpty()) ? null : format;
        LocalDateTime now = LocalDateTime.now();
        if ("CURRENT_TIMESTAMP".equalsIgnoreCase(key) || "GETDATE".equalsIgnoreCase(key) || "GETDATE()".equalsIgnoreCase(key)) {
            String p = (pattern == null) ? "yyyy-MM-dd HH:mm:ss" : pattern;
            return now.format(DateTimeFormatter.ofPattern(convertNodeDatePattern(p)));
        }
        if ("CURRENT_DATE".equalsIgnoreCase(key)) {
            String p = (pattern == null) ? "yyyy-MM-dd" : pattern;
            return now.format(DateTimeFormatter.ofPattern(convertNodeDatePattern(p)));
        }
        if ("CURRENT_TIME".equalsIgnoreCase(key)) {
            String p = (pattern == null) ? "HH:mm:ss" : pattern;
            return now.format(DateTimeFormatter.ofPattern(convertNodeDatePattern(p)));
        }
        if ("UNIX_TIMESTAMP".equalsIgnoreCase(key)) {
            return String.valueOf(Instant.now().getEpochSecond());
        }
        if ("TODAY".equalsIgnoreCase(key)) {
            return now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
        return null;
    }

    private String formatDate(String dateKey, String format) {
        String pattern = (format == null || format.isEmpty()) ? "yyyyMMddHHmmss" : format;
        String tz = "UTC";
        String base = dateKey;
        int dotIndex = dateKey.indexOf('.');
        if (dotIndex >= 0) {
            base = dateKey.substring(0, dotIndex);
            tz = dateKey.substring(dotIndex + 1).toUpperCase();
        }
        if (!"DATE".equalsIgnoreCase(base)) {
            return "${" + dateKey + "}";
        }

        Integer offsetMinutes = TIMEZONE_OFFSETS.get(tz);
        if (offsetMinutes == null) {
            return "${" + dateKey + "}";
        }

        ZoneOffset offset = ZoneOffset.ofTotalSeconds(offsetMinutes*60);
        ZonedDateTime now = ZonedDateTime.of(LocalDateTime.now(), offset);

        String javaPattern = convertNodeDatePattern(pattern);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(javaPattern, Locale.getDefault());
        return now.format(formatter);
    }

    private String convertNodeDatePattern(String pattern) {
        String result = pattern;
        for (String[] token : TOKEN_MAP) {
            result = result.replace(token[0], token[1]);
        }
        return result;
    }
}
