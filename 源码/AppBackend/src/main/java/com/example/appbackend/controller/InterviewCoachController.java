package com.example.appbackend.controller;

import com.example.appbackend.service.InterviewCoachService;
import com.example.appbackend.util.InterviewAuthSupport;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
public class InterviewCoachController {

    private final InterviewCoachService coachService;

    public InterviewCoachController(InterviewCoachService coachService) {
        this.coachService = coachService;
    }

    @PostMapping(value = "/api/ai-coach", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter aiCoach(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        return coachService.coach(InterviewAuthSupport.requireUserId(request), body,
                InterviewAuthSupport.authorization(request));
    }

    @PostMapping(value = "/api/ai-custom-path", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter customPath(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        return coachService.customPath(InterviewAuthSupport.requireUserId(request), body,
                InterviewAuthSupport.authorization(request));
    }
}
