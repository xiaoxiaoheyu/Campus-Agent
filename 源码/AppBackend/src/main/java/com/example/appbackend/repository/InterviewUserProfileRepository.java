package com.example.appbackend.repository;

import com.example.appbackend.entity.InterviewUserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InterviewUserProfileRepository extends JpaRepository<InterviewUserProfile, Long> {
    Optional<InterviewUserProfile> findByUserId(Long userId);
}
