package com.example.appbackend.repository;

import com.example.appbackend.entity.InterviewJobTechStack;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterviewJobTechStackRepository extends JpaRepository<InterviewJobTechStack, Long> {
    List<InterviewJobTechStack> findByStatusOrderByIdAsc(Integer status);
}
