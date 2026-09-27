package com.example.appbackend.repository;

import com.example.appbackend.entity.UserQuestionAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserQuestionAttemptRepository extends JpaRepository<UserQuestionAttempt, Long> {
    List<UserQuestionAttempt> findByUserIdAndKnowledgeIdOrderByCreatedAtDesc(Long userId, Long knowledgeId);
    Optional<UserQuestionAttempt> findFirstByUserIdAndKnowledgeIdOrderByCreatedAtDesc(Long userId, Long knowledgeId);
}
