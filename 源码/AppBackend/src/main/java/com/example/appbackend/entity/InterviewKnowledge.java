package com.example.appbackend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "knowledge_base")
public class InterviewKnowledge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_position", nullable = false, length = 100)
    private String jobPosition;

    @Column(name = "question_type", nullable = false, length = 50)
    private String questionType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(name = "excellent_answer", columnDefinition = "TEXT")
    private String excellentAnswer;

    @Column(nullable = false)
    private Integer difficulty = 3;

    @Column(length = 255)
    private String keywords;

    @Column(name = "question_intent", length = 255)
    private String questionIntent;

    @Column(name = "answer_points", columnDefinition = "TEXT")
    private String answerPoints;

    @Column(name = "score_standard", columnDefinition = "TEXT")
    private String scoreStandard;

    @Column(name = "suitable_level", length = 50)
    private String suitableLevel;

    @Column(length = 255)
    private String remark;

    @Column(nullable = false)
    private Integer status = 1;

    @Column(name = "in_chroma", nullable = false)
    private Integer inChroma = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (difficulty == null) difficulty = 3;
        if (status == null) status = 1;
        if (inChroma == null) inChroma = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
