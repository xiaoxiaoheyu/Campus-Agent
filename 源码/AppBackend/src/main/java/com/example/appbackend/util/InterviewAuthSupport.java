package com.example.appbackend.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.LinkedHashMap;
import java.util.Map;

public final class InterviewAuthSupport {

    private InterviewAuthSupport() {
    }

    public static Long resolveUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("userId");
        if (attr instanceof Long l) {
            return l;
        }
        if (attr instanceof Number n) {
            return n.longValue();
        }
        if (attr != null) {
            try {
                return Long.parseLong(String.valueOf(attr));
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    public static Long requireUserId(HttpServletRequest request) {
        Long userId = resolveUserId(request);
        if (userId == null || userId <= 0) {
            throw new InterviewApiException(HttpStatus.UNAUTHORIZED, "invalid_or_expired_session");
        }
        return userId;
    }

    public static String authorization(HttpServletRequest request) {
        return request.getHeader("Authorization");
    }

    public static ResponseEntity<Map<String, Object>> error(HttpStatus status, String code) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", code);
        return ResponseEntity.status(status).body(body);
    }

    public static ResponseEntity<Map<String, Object>> error(HttpStatus status, String code, String field) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", code);
        if (field != null) {
            body.put("field", field);
        }
        return ResponseEntity.status(status).body(body);
    }

    public static ResponseEntity<Map<String, Object>> error(HttpStatus status, String code, Map<String, Object> extra) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", code);
        if (extra != null) {
            body.putAll(extra);
        }
        return ResponseEntity.status(status).body(body);
    }

    public static class InterviewApiException extends RuntimeException {
        private final HttpStatus status;
        private final String code;
        private final Map<String, Object> extra;

        public InterviewApiException(HttpStatus status, String code) {
            this(status, code, null);
        }

        public InterviewApiException(HttpStatus status, String code, Map<String, Object> extra) {
            super(code);
            this.status = status;
            this.code = code;
            this.extra = extra;
        }

        public HttpStatus getStatus() {
            return status;
        }

        public String getCode() {
            return code;
        }

        public Map<String, Object> getExtra() {
            return extra;
        }
    }
}
