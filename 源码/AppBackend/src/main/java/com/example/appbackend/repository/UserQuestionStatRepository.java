package com.example.appbackend.repository;

import com.example.appbackend.entity.UserQuestionStat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserQuestionStatRepository extends JpaRepository<UserQuestionStat, Long> {
    Optional<UserQuestionStat> findByUserIdAndKnowledgeId(Long userId, Long knowledgeId);
    List<UserQuestionStat> findByUserIdAndIsWrongBookOrderByUpdatedAtDesc(Long userId, Integer isWrongBook);
}
