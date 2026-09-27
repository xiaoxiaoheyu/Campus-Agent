package com.example.appbackend.repository;

import com.example.appbackend.entity.InterviewEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InterviewEvaluationRepository extends JpaRepository<InterviewEvaluation, Long> {
    Optional<InterviewEvaluation> findFirstByConversationIdOrderByIdDesc(String conversationId);
    List<InterviewEvaluation> findByUserIdOrderByIdDesc(Long userId);
    boolean existsByConversationId(String conversationId);
}
