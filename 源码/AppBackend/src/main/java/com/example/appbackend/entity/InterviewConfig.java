package com.example.appbackend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "interview_config")
public class InterviewConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_pk")
    private Long conversationPk;

    @Column(name = "conversation_id", length = 64)
    private String conversationId;

    @Column(name = "target_position", nullable = false, length = 100)
    private String targetPosition;

    @Column(name = "interview_mode", length = 20, nullable = false)
    private String interviewMode = "full";

    @Column(name = "interviewer_persona", length = 20, nullable = false)
    private String interviewerPersona = "neutral";

    @Column(name = "focus_tags", length = 255)
    private String focusTags;

    @Column(columnDefinition = "TEXT")
    private String requirements;

    @Column(nullable = false)
    private Integer difficulty = 3;

    @Column(length = 255)
    private String remark;

    @Column(nullable = false)
    private Integer status = 1;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (interviewMode == null) interviewMode = "full";
        if (interviewerPersona == null) interviewerPersona = "neutral";
        if (difficulty == null) difficulty = 3;
        if (status == null) status = 1;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
