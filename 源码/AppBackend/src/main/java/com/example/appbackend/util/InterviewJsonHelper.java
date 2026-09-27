package com.example.appbackend.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class InterviewJsonHelper {

    private static final Pattern JSON_OBJ = Pattern.compile("\\{[\\s\\S]*\\}");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private InterviewJsonHelper() {
    }

    public static Map<String, Object> extractJsonObject(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            Map<String, Object> parsed = MAPPER.readValue(text, new TypeReference<>() {});
            if (parsed != null) return parsed;
        } catch (Exception ignored) {
        }
        Matcher m = JSON_OBJ.matcher(text);
        if (!m.find()) {
            return Collections.emptyMap();
        }
        try {
            Map<String, Object> parsed = MAPPER.readValue(m.group(0), new TypeReference<>() {});
            return parsed != null ? parsed : Collections.emptyMap();
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    public static String asText(Object value, String defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof List<?> list) {
            StringBuilder sb = new StringBuilder();
            for (Object x : list) {
                if (x == null) continue;
                String s = String.valueOf(x).trim();
                if (s.isEmpty()) continue;
                if (!sb.isEmpty()) sb.append('\n');
                sb.append(s);
            }
            return sb.isEmpty() ? defaultValue : sb.toString();
        }
        String s = String.valueOf(value).trim();
        return s.isEmpty() ? defaultValue : s;
    }

    public static int toScore(Object v) {
        try {
            int score = (int) Math.round(Double.parseDouble(String.valueOf(v).trim()));
            if (score < 0) return 0;
            if (score > 100) return 100;
            return score;
        } catch (Exception e) {
            return 0;
        }
    }

    public static Integer asInt(Object v, Integer def) {
        if (v == null) return def;
        try {
            return Integer.parseInt(String.valueOf(v).trim());
        } catch (Exception e) {
            return def;
        }
    }

    public static String asStr(Object v) {
        return v == null ? null : String.valueOf(v);
    }
}
