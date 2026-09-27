package com.example.appbackend.controller;

import com.example.appbackend.service.InterviewConfigApiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/interview-config")
public class InterviewConfigApiController {

    private final InterviewConfigApiService configApiService;

    public InterviewConfigApiController(InterviewConfigApiService configApiService) {
        this.configApiService = configApiService;
    }

    @GetMapping("/get")
    public ResponseEntity<?> get(@RequestParam(value = "id", required = false) Long id,
                                 @RequestParam(value = "conversation_id", required = false) String conversationId) {
        return ResponseEntity.ok(configApiService.get(id, conversationId));
    }

    @RequestMapping(value = "/update", method = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH})
    public ResponseEntity<?> update(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(configApiService.update(body));
    }
}
