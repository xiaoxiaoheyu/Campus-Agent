package com.example.appbackend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "user_question_stat",
       uniqueConstraints = @UniqueConstraint(name = "uq_user_knowledge_stat", columnNames = {"user_id", "knowledge_id"}))
public class UserQuestionStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "knowledge_id", nullable = false)
    private Long knowledgeId;

    @Column(name = "total_attempts", nullable = false)
    private Integer totalAttempts = 0;

    @Column(name = "wrong_attempts", nullable = false)
    private Integer wrongAttempts = 0;

    @Column(name = "latest_score", nullable = false)
    private Integer latestScore = 0;

    @Column(name = "best_score", nullable = false)
    private Integer bestScore = 0;

    @Column(name = "avg_score", precision = 5, scale = 2, nullable = false)
    private BigDecimal avgScore = BigDecimal.ZERO;

    @Column(name = "is_wrong_book", nullable = false)
    private Integer isWrongBook = 0;

    @Column(name = "wrong_streak", nullable = false)
    private Integer wrongStreak = 0;

    @Column(name = "first_attempt_at")
    private LocalDateTime firstAttemptAt;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (totalAttempts == null) totalAttempts = 0;
        if (wrongAttempts == null) wrongAttempts = 0;
        if (latestScore == null) latestScore = 0;
        if (bestScore == null) bestScore = 0;
        if (avgScore == null) avgScore = BigDecimal.ZERO;
        if (isWrongBook == null) isWrongBook = 0;
        if (wrongStreak == null) wrongStreak = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
