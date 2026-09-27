package com.example.appbackend.repository;

import com.example.appbackend.entity.InterviewConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InterviewConfigRepository extends JpaRepository<InterviewConfig, Long> {
    Optional<InterviewConfig> findFirstByConversationIdOrderByIdDesc(String conversationId);
}
