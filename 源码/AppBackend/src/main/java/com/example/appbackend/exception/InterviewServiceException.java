package com.example.appbackend.exception;

import lombok.Getter;

import java.util.Collections;
import java.util.Map;

@Getter
public class InterviewServiceException extends RuntimeException {

    private final String code;
    private final int httpStatus;
    private final Map<String, Object> meta;

    public InterviewServiceException(String code, int httpStatus) {
        this(code, httpStatus, Collections.emptyMap());
    }

    public InterviewServiceException(String code, int httpStatus, Map<String, Object> meta) {
        super(code);
        this.code = code;
        this.httpStatus = httpStatus;
        this.meta = meta == null ? Collections.emptyMap() : meta;
    }
}
