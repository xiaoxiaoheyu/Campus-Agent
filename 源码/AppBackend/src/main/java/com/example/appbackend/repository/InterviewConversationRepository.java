package com.example.appbackend.repository;

import com.example.appbackend.entity.InterviewConversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InterviewConversationRepository extends JpaRepository<InterviewConversation, Long> {
    Optional<InterviewConversation> findByConversationId(String conversationId);
    boolean existsByConversationId(String conversationId);

    @Query("SELECT c FROM InterviewConversation c WHERE c.userId = :userId AND "
            + "(:status IS NULL OR :status = '' OR c.status = :status) AND "
            + "(:jobRole IS NULL OR :jobRole = '' OR c.jobRole = :jobRole)")
    Page<InterviewConversation> search(@Param("userId") Long userId,
                                       @Param("status") String status,
                                       @Param("jobRole") String jobRole,
                                       Pageable pageable);
}
