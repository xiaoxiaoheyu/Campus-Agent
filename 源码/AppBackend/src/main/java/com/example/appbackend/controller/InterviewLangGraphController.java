package com.example.appbackend.controller;

import com.example.appbackend.service.InterviewLangGraphService;
import com.example.appbackend.util.InterviewAuthSupport;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
public class InterviewLangGraphController {

    private final InterviewLangGraphService langGraphService;

    public InterviewLangGraphController(InterviewLangGraphService langGraphService) {
        this.langGraphService = langGraphService;
    }

    @PostMapping(value = "/api/langgraph/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        return langGraphService.chat(body, InterviewAuthSupport.authorization(request));
    }
}
