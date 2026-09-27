package com.example.appbackend.controller;

import com.example.appbackend.service.InterviewKnowledgeService;
import com.example.appbackend.util.InterviewAuthSupport;
import com.example.appbackend.util.InterviewJsonHelper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/knowledge")
public class InterviewKnowledgeController {

    private final InterviewKnowledgeService knowledgeService;

    public InterviewKnowledgeController(InterviewKnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                knowledgeService.create(body, InterviewAuthSupport.resolveUserId(request)));
    }

    @GetMapping("/detail")
    public ResponseEntity<?> detail(@RequestParam("id") Long id) {
        return ResponseEntity.ok(knowledgeService.detail(id));
    }

    @RequestMapping(value = "/update", method = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH})
    public ResponseEntity<?> update(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        return ResponseEntity.ok(knowledgeService.update(body, InterviewAuthSupport.resolveUserId(request)));
    }

    @RequestMapping(value = "/delete", method = {RequestMethod.POST, RequestMethod.DELETE})
    public ResponseEntity<?> delete(@RequestBody(required = false) Map<String, Object> body,
                                    @RequestParam(value = "id", required = false) Long id,
                                    HttpServletRequest request) {
        Long resolved = id;
        if (resolved == null && body != null) {
            Integer v = InterviewJsonHelper.asInt(body.get("id"), null);
            if (v != null) resolved = v.longValue();
        }
        return ResponseEntity.ok(knowledgeService.delete(resolved, body, InterviewAuthSupport.resolveUserId(request)));
    }

    @GetMapping("/list")
    public ResponseEntity<?> list(@RequestParam(value = "page", defaultValue = "1") int page,
                                  @RequestParam(value = "page_size", defaultValue = "10") int pageSize,
                                  @RequestParam(value = "job_position", required = false) String jobPosition,
                                  @RequestParam(value = "question_type", required = false) String questionType,
                                  @RequestParam(value = "q", required = false) String q,
                                  @RequestParam(value = "status", required = false) Integer status,
                                  @RequestParam(value = "is_manager", required = false) Integer isManager,
                                  HttpServletRequest request) {
        Map<String, Object> filters = new HashMap<>();
        if (isManager != null) filters.put("is_manager", isManager);
        return ResponseEntity.ok(knowledgeService.list(jobPosition, questionType, q, status, page, pageSize,
                InterviewAuthSupport.resolveUserId(request), filters));
    }

    @GetMapping("/types")
    public ResponseEntity<?> types(@RequestParam(value = "job_position", required = false) String jobPosition,
                                   @RequestParam(value = "status", required = false) Integer status) {
        return ResponseEntity.ok(knowledgeService.types(jobPosition, status));
    }

    @GetMapping("/positions")
    public ResponseEntity<?> positions(@RequestParam(value = "question_type", required = false) String questionType,
                                       @RequestParam(value = "status", required = false) Integer status) {
        return ResponseEntity.ok(knowledgeService.positions(questionType, status));
    }

    @PostMapping("/ai-recognize")
    public ResponseEntity<?> aiRecognize(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        return ResponseEntity.ok(knowledgeService.aiRecognize(body, InterviewAuthSupport.authorization(request)));
    }

    @PostMapping("/grade")
    public ResponseEntity<?> grade(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        return ResponseEntity.ok(knowledgeService.grade(body, InterviewAuthSupport.requireUserId(request),
                InterviewAuthSupport.authorization(request)));
    }
}
