package com.example.appbackend.service;

import com.example.appbackend.entity.AiInterviewMessage;
import com.example.appbackend.entity.InterviewConfig;
import com.example.appbackend.entity.InterviewConversation;
import com.example.appbackend.exception.InterviewServiceException;
import com.example.appbackend.repository.AiInterviewMessageRepository;
import com.example.appbackend.repository.InterviewConfigRepository;
import com.example.appbackend.repository.InterviewConversationRepository;
import com.example.appbackend.util.InterviewJsonHelper;
import com.example.appbackend.util.InterviewMaps;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
public class InterviewMessageService {

    private final AiInterviewMessageRepository messageRepository;
    private final InterviewConversationRepository conversationRepository;

    public InterviewMessageService(AiInterviewMessageRepository messageRepository,
                                   InterviewConversationRepository conversationRepository) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
    }

    @Transactional
    public Map<String, Object> add(Map<String, Object> body) {
        String conversationId = InterviewJsonHelper.asStr(body.get("conversation_id"));
        String role = InterviewJsonHelper.asStr(body.get("role"));
        String content = InterviewJsonHelper.asStr(body.get("content"));
        if (!StringUtils.hasText(conversationId) || !StringUtils.hasText(role) || !StringUtils.hasText(content)) {
            throw new InterviewServiceException("missing_or_empty_field", 400);
        }
        InterviewConversation c = conversationRepository.findByConversationId(conversationId)
                .orElseThrow(() -> new InterviewServiceException("conversation_not_found", 400));
        AiInterviewMessage msg = new AiInterviewMessage();
        msg.setConversationPk(c.getId());
        msg.setConversationId(conversationId);
        msg.setRole(role);
        msg.setContent(content);
        msg.setEmotionLabel(InterviewJsonHelper.asStr(body.get("emotion_label")));
        msg = messageRepository.save(msg);
        return InterviewMaps.messageItem(msg);
    }

    public Map<String, Object> list(String conversationId) {
        List<Map<String, Object>> items = messageRepository.findByConversationIdOrderByIdAsc(conversationId).stream()
                .map(InterviewMaps::messageItem)
                .toList();
        return Map.of("items", items);
    }

    public Map<String, Object> history(String conversationId) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (AiInterviewMessage msg : messageRepository.findByConversationIdOrderByIdAsc(conversationId)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("role", msg.getRole());
            m.put("content", msg.getContent());
            m.put("created_at", InterviewMaps.iso(msg.getCreatedAt()));
            items.add(m);
        }
        return Map.of("items", items);
    }
}
