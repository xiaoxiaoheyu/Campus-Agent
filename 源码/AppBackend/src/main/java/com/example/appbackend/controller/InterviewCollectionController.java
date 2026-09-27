package com.example.appbackend.controller;

import com.example.appbackend.service.InterviewCollectionService;
import com.example.appbackend.util.InterviewAuthSupport;
import com.example.appbackend.util.InterviewJsonHelper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/collection")
public class InterviewCollectionController {

    private final InterviewCollectionService collectionService;

    public InterviewCollectionController(InterviewCollectionService collectionService) {
        this.collectionService = collectionService;
    }

    @GetMapping("/list")
    public ResponseEntity<?> list(@RequestParam(value = "q", required = false) String q,
                                  @RequestParam(value = "job_position", required = false) String jobPosition,
                                  @RequestParam(value = "knowledge_id", required = false) Long knowledgeId,
                                  HttpServletRequest request) {
        return ResponseEntity.ok(collectionService.list(InterviewAuthSupport.requireUserId(request), q, jobPosition, knowledgeId));
    }

    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                collectionService.create(InterviewAuthSupport.requireUserId(request), body));
    }

    @RequestMapping(value = "/update-remark", method = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH})
    public ResponseEntity<?> updateRemark(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        return ResponseEntity.ok(collectionService.updateRemark(InterviewAuthSupport.requireUserId(request), body));
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
        return ResponseEntity.ok(collectionService.delete(InterviewAuthSupport.requireUserId(request), resolved));
    }
}
