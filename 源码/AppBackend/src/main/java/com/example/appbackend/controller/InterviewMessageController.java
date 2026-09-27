package com.example.appbackend.controller;

import com.example.appbackend.service.InterviewMessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/message")
public class InterviewMessageController {

    private final InterviewMessageService messageService;

    public InterviewMessageController(InterviewMessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping("/add")
    public ResponseEntity<?> add(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(messageService.add(body));
    }

    @GetMapping("/list")
    public ResponseEntity<?> list(@RequestParam("conversation_id") String conversationId) {
        return ResponseEntity.ok(messageService.list(conversationId));
    }
}
