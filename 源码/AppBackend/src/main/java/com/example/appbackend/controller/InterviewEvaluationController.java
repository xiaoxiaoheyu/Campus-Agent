package com.example.appbackend.controller;

import com.example.appbackend.service.InterviewEvaluationService;
import com.example.appbackend.util.InterviewAuthSupport;
import com.example.appbackend.util.InterviewJsonHelper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/interview/evaluation")
public class InterviewEvaluationController {

    private final InterviewEvaluationService evaluationService;

    public InterviewEvaluationController(InterviewEvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @PostMapping("/summarize")
    public ResponseEntity<?> summarize(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        String conversationId = InterviewJsonHelper.asStr(body.get("conversation_id"));
        return ResponseEntity.status(HttpStatus.CREATED).body(
                evaluationService.summarize(conversationId, InterviewAuthSupport.authorization(request)));
    }
}
