package com.example.appbackend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "interview_user_profile")
public class InterviewUserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(length = 50)
    private String nickname;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    private Integer gender = 0;

    @Column(name = "work_experience_years")
    private Integer workExperienceYears = 0;

    @Column(name = "work_experience", columnDefinition = "TEXT")
    private String workExperience;

    @Column(length = 20)
    private String education = "bachelor";

    @Column(length = 100)
    private String major;

    @Column(name = "graduation_year")
    private Integer graduationYear;

    @Column(length = 100)
    private String school;

    @Column(name = "target_position", length = 100)
    private String targetPosition;

    @Column(name = "skill_tags", length = 255)
    private String skillTags;

    @Column(name = "tech_stack", columnDefinition = "TEXT")
    private String techStack;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "resume_url", length = 500)
    private String resumeUrl;

    @Column(name = "interview_count", nullable = false)
    private Integer interviewCount = 0;

    @Column(name = "ai_report_count", nullable = false)
    private Integer aiReportCount = 0;

    @Column(name = "average_score", precision = 5, scale = 2, nullable = false)
    private BigDecimal averageScore = BigDecimal.ZERO;

    @Column(name = "is_manager", nullable = false)
    private Integer isManager = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (gender == null) gender = 0;
        if (workExperienceYears == null) workExperienceYears = 0;
        if (education == null) education = "bachelor";
        if (interviewCount == null) interviewCount = 0;
        if (aiReportCount == null) aiReportCount = 0;
        if (averageScore == null) averageScore = BigDecimal.ZERO;
        if (isManager == null) isManager = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
