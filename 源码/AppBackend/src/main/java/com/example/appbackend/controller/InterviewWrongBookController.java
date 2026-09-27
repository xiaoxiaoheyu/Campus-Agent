package com.example.appbackend.controller;

import com.example.appbackend.service.InterviewCollectionService;
import com.example.appbackend.util.InterviewAuthSupport;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wrong-book")
public class InterviewWrongBookController {

    private final InterviewCollectionService collectionService;

    public InterviewWrongBookController(InterviewCollectionService collectionService) {
        this.collectionService = collectionService;
    }

    @GetMapping("/list")
    public ResponseEntity<?> list(HttpServletRequest request) {
        return ResponseEntity.ok(collectionService.wrongBookList(InterviewAuthSupport.requireUserId(request)));
    }
}
