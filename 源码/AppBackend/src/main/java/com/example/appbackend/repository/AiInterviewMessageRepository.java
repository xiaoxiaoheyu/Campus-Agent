package com.example.appbackend.repository;

import com.example.appbackend.entity.AiInterviewMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiInterviewMessageRepository extends JpaRepository<AiInterviewMessage, Long> {
    List<AiInterviewMessage> findByConversationIdOrderByIdAsc(String conversationId);
    List<AiInterviewMessage> findByConversationPkOrderByIdAsc(Long conversationPk);
}
