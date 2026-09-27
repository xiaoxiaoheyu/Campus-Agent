package com.example.appbackend.controller;

import com.example.appbackend.service.InterviewUserProfileService;
import com.example.appbackend.util.InterviewAuthSupport;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class InterviewUserController {

    private final InterviewUserProfileService profileService;

    public InterviewUserController(InterviewUserProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/detail")
    public ResponseEntity<?> detail(@RequestParam(value = "id", required = false) Long id,
                                    @RequestParam(value = "user_id", required = false) Long userId,
                                    HttpServletRequest request) {
        Long queryId = id != null ? id : userId;
        return ResponseEntity.ok(profileService.detail(queryId, InterviewAuthSupport.resolveUserId(request)));
    }

    @RequestMapping(value = "/update", method = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH})
    public ResponseEntity<?> update(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(profileService.update(body));
    }

    @RequestMapping(value = "/update-metrics", method = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH})
    public ResponseEntity<?> updateMetrics(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(profileService.updateMetrics(body));
    }

    @GetMapping("/interview-job-positions")
    public ResponseEntity<?> jobPositions() {
        return ResponseEntity.ok(profileService.jobPositions());
    }

    @PostMapping("/upload-avatar")
    public ResponseEntity<?> uploadAvatar(@RequestParam(value = "file", required = false) MultipartFile file,
                                          @RequestParam(value = "avatar", required = false) MultipartFile avatar,
                                          @RequestParam(value = "id", required = false) Long id,
                                          HttpServletRequest request) {
        Long userId = InterviewAuthSupport.resolveUserId(request);
        if (userId == null || userId <= 0) userId = id;
        if (userId == null || userId <= 0) {
            return InterviewAuthSupport.error(org.springframework.http.HttpStatus.UNAUTHORIZED, "invalid_or_expired_session");
        }
        MultipartFile upload = file != null ? file : avatar;
        return ResponseEntity.ok(profileService.uploadAvatar(userId, upload));
    }
}
