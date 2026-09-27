package com.example.appbackend.repository;

import com.example.appbackend.entity.UserQuestionCollection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserQuestionCollectionRepository extends JpaRepository<UserQuestionCollection, Long> {
    List<UserQuestionCollection> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<UserQuestionCollection> findByUserIdAndKnowledgeId(Long userId, Long knowledgeId);
    Optional<UserQuestionCollection> findByIdAndUserId(Long id, Long userId);
}
