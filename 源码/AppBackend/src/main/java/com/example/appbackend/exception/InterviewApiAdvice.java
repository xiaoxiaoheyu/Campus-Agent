package com.example.appbackend.exception;

import com.example.appbackend.util.InterviewAuthSupport.InterviewApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(basePackages = "com.example.appbackend.controller")
public class InterviewApiAdvice {

    @ExceptionHandler(InterviewApiException.class)
    public ResponseEntity<Map<String, Object>> handle(InterviewApiException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", ex.getCode());
        if (ex.getExtra() != null) {
            body.putAll(ex.getExtra());
        }
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler(InterviewServiceException.class)
    public ResponseEntity<Map<String, Object>> handleService(InterviewServiceException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", ex.getCode());
        if (ex.getMeta() != null) {
            body.putAll(ex.getMeta());
        }
        HttpStatus status = HttpStatus.resolve(ex.getHttpStatus());
        if (status == null) status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(body);
    }
}
