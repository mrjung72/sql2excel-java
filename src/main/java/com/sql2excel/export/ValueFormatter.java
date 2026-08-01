package com.sql2excel.export;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class ValueFormatter {

    public static Object formatValue(Object value, String dateColumnFormat) {
        if (value == null) {
            return null;
        }
        if (isDateValue(value)) {
            String pattern = (dateColumnFormat != null && !dateColumnFormat.isEmpty())
                    ? dateColumnFormat
                    : defaultDatePattern(value);
            return formatDate(value, pattern);
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value;
        }
        return value.toString();
    }

    public static boolean isDateValue(Object value) {
        return value instanceof java.util.Date
                || value instanceof LocalDateTime
                || value instanceof LocalDate;
    }

    private static String defaultDatePattern(Object value) {
        if (value instanceof java.sql.Date || value instanceof LocalDate) {
            return "yyyy-MM-dd";
        }
        return "yyyy-MM-dd HH:mm:ss";
    }

    private static String formatDate(Object value, String pattern) {
        try {
            if (value instanceof java.util.Date) {
                SimpleDateFormat sdf = new SimpleDateFormat(pattern);
                return sdf.format((java.util.Date) value);
            } else if (value instanceof LocalDateTime) {
                return ((LocalDateTime) value).format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()));
            } else if (value instanceof LocalDate) {
                return ((LocalDate) value).format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()));
            }
        } catch (Exception ignored) {
        }
        return value.toString();
    }
}
