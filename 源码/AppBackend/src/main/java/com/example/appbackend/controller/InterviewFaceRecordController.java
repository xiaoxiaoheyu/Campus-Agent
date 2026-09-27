package com.example.appbackend.controller;

import com.example.appbackend.service.InterviewFaceRecordService;
import com.example.appbackend.util.InterviewJsonHelper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/face/record")
public class InterviewFaceRecordController {

    private final InterviewFaceRecordService faceRecordService;

    public InterviewFaceRecordController(InterviewFaceRecordService faceRecordService) {
        this.faceRecordService = faceRecordService;
    }

    @PostMapping("/start")
    public ResponseEntity<?> start(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(faceRecordService.start(body));
    }

    @PostMapping("/append")
    public ResponseEntity<?> append(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(faceRecordService.append(body));
    }

    @PostMapping("/end")
    public ResponseEntity<?> end(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(faceRecordService.end(body));
    }

    @GetMapping("/list")
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(faceRecordService.list());
    }

    @GetMapping("/detail/{conversationId}")
    public ResponseEntity<?> detail(@PathVariable String conversationId) {
        return ResponseEntity.ok(faceRecordService.detail(conversationId));
    }

    @RequestMapping(value = {"/emotion-summary", "/emotion-summary/{conversationId}"}, method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<?> emotionSummary(@PathVariable(value = "conversationId", required = false) String pathId,
                                            @RequestParam(value = "conversation_id", required = false) String queryId,
                                            @RequestParam(value = "smooth_window", defaultValue = "5") int smoothWindow,
                                            @RequestParam(value = "neutral_margin", defaultValue = "0.10") double neutralMargin,
                                            @RequestParam(value = "min_confidence", defaultValue = "0.32") double minConfidence,
                                            @RequestBody(required = false) Map<String, Object> body) {
        String id = pathId;
        if (id == null || id.isBlank()) id = queryId;
        if ((id == null || id.isBlank()) && body != null) {
            id = InterviewJsonHelper.asStr(body.get("conversation_id"));
        }
        return ResponseEntity.ok(faceRecordService.emotionSummary(id, smoothWindow, neutralMargin, minConfidence));
    }
}
