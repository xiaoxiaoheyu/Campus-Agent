package com.example.appbackend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "ai_interview_message")
public class AiInterviewMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_pk", nullable = false)
    private Long conversationPk;

    @Column(name = "conversation_id", nullable = false, length = 64)
    private String conversationId;

    @Column(nullable = false, length = 50)
    private String role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "emotion_label", length = 50)
    private String emotionLabel;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
