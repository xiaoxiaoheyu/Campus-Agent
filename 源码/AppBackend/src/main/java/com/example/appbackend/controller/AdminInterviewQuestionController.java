package com.example.appbackend.controller;

import com.example.appbackend.entity.InterviewKnowledge;
import com.example.appbackend.entity.Result;
import com.example.appbackend.repository.InterviewKnowledgeRepository;
import com.example.appbackend.service.InterviewKnowledgeService;
import com.example.appbackend.util.InterviewAuthSupport;
import com.example.appbackend.util.InterviewMaps;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/interview/questions")
public class AdminInterviewQuestionController {

    private final InterviewKnowledgeService knowledgeService;
    private final InterviewKnowledgeRepository knowledgeRepository;

    public AdminInterviewQuestionController(InterviewKnowledgeService knowledgeService,
                                            InterviewKnowledgeRepository knowledgeRepository) {
        this.knowledgeService = knowledgeService;
        this.knowledgeRepository = knowledgeRepository;
    }

    @GetMapping
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "20") int pageSize,
                          @RequestParam(required = false) String q,
                          @RequestParam(required = false) String jobPosition,
                          @RequestParam(required = false) String questionType,
                          HttpServletRequest request) {
        Map<String, Object> filters = new HashMap<>();
        filters.put("is_manager", 1);
        return Result.success(knowledgeService.list(jobPosition, questionType, q, null, page, pageSize,
                InterviewAuthSupport.resolveUserId(request), filters));
    }

    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return Result.success(knowledgeService.detail(id));
    }

    @PostMapping
    public Result<?> create(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        body.put("is_manager", 1);
        return Result.success(knowledgeService.create(body, InterviewAuthSupport.resolveUserId(request)));
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        body.put("id", id);
        body.put("is_manager", 1);
        return Result.success(knowledgeService.update(body, InterviewAuthSupport.resolveUserId(request)));
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id, HttpServletRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("is_manager", 1);
        return Result.success(knowledgeService.delete(id, body, InterviewAuthSupport.resolveUserId(request)));
    }
}
