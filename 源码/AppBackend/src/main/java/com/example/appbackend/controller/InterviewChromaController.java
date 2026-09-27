package com.example.appbackend.controller;

import com.example.appbackend.entity.InterviewUserProfile;
import com.example.appbackend.exception.InterviewServiceException;
import com.example.appbackend.repository.InterviewUserProfileRepository;
import com.example.appbackend.service.ChromaInterviewService;
import com.example.appbackend.util.InterviewAuthSupport;
import com.example.appbackend.util.InterviewJsonHelper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/chroma")
public class InterviewChromaController {

    private final ChromaInterviewService chromaInterviewService;
    private final InterviewUserProfileRepository profileRepository;

    public InterviewChromaController(ChromaInterviewService chromaInterviewService,
                                     InterviewUserProfileRepository profileRepository) {
        this.chromaInterviewService = chromaInterviewService;
        this.profileRepository = profileRepository;
    }

    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        Long id = Long.valueOf(InterviewJsonHelper.asInt(body.get("id"), 0));
        return ResponseEntity.ok(chromaInterviewService.create(id));
    }

    @GetMapping("/match")
    public ResponseEntity<?> match(@RequestParam("q") String q,
                                   @RequestParam(value = "k", defaultValue = "5") int k,
                                   @RequestParam(value = "job_position", required = false) String jobPosition,
                                   @RequestParam(value = "question_type", required = false) String questionType,
                                   @RequestParam(value = "status", required = false) Integer status,
                                   HttpServletRequest request) {
        Long userId = InterviewAuthSupport.requireUserId(request);
        InterviewUserProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new InterviewServiceException("forbidden", 403));
        if (!Objects.equals(profile.getIsManager(), 1)) {
            throw new InterviewServiceException("forbidden", 403);
        }
        return ResponseEntity.ok(chromaInterviewService.match(q, k, jobPosition, questionType, status));
    }
}
