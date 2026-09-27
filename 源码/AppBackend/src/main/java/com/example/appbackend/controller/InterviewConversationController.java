package com.example.appbackend.controller;

import com.example.appbackend.exception.InterviewServiceException;
import com.example.appbackend.service.InterviewConversationService;
import com.example.appbackend.service.InterviewMessageService;
import com.example.appbackend.util.InterviewAuthSupport;
import com.example.appbackend.util.InterviewJsonHelper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/conversation")
public class InterviewConversationController {

    private final InterviewConversationService conversationService;
    private final InterviewMessageService messageService;

    public InterviewConversationController(InterviewConversationService conversationService,
                                           InterviewMessageService messageService) {
        this.conversationService = conversationService;
        this.messageService = messageService;
    }

    @PostMapping("/new-id")
    public ResponseEntity<?> newId(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        return ResponseEntity.ok(conversationService.newId(body == null ? Map.of() : body,
                InterviewAuthSupport.resolveUserId(request)));
    }

    @PostMapping("/start")
    public ResponseEntity<?> start(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(conversationService.start(body));
    }

    @PostMapping("/end")
    public ResponseEntity<?> end(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(conversationService.end(body));
    }

    @PostMapping("/commit")
    public ResponseEntity<?> commit(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(conversationService.commit(body));
    }

    @GetMapping("/list")
    public ResponseEntity<?> list(@RequestParam(value = "page", required = false) Integer page,
                                  @RequestParam(value = "page_size", required = false) Integer pageSize,
                                  @RequestParam(value = "status", required = false) String status,
                                  @RequestParam(value = "job_role", required = false) String jobRole,
                                  @RequestParam(value = "order_by", required = false) String orderBy,
                                  @RequestParam(value = "user_id", required = false) Long userId,
                                  HttpServletRequest request) {
        Long jwtUserId = InterviewAuthSupport.requireUserId(request);
        if (userId != null && !Objects.equals(userId, jwtUserId)) {
            throw new InterviewServiceException("forbidden_user_scope", 403);
        }
        return ResponseEntity.ok(conversationService.list(jwtUserId, page, pageSize, status, jobRole, orderBy));
    }

    @GetMapping("/history")
    public ResponseEntity<?> history(@RequestParam("conversation_id") String conversationId) {
        return ResponseEntity.ok(messageService.history(conversationId));
    }
}
