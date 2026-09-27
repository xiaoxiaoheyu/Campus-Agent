package com.example.appbackend.service;

import com.example.appbackend.dto.LlmChatRequest;
import com.example.appbackend.dto.LlmChatResponse;
import com.example.appbackend.entity.AiInterviewMessage;
import com.example.appbackend.entity.InterviewConfig;
import com.example.appbackend.entity.InterviewConversation;
import com.example.appbackend.entity.InterviewUserProfile;
import com.example.appbackend.exception.InterviewServiceException;
import com.example.appbackend.prompt.InterviewPrompts;
import com.example.appbackend.repository.AiInterviewMessageRepository;
import com.example.appbackend.repository.InterviewConfigRepository;
import com.example.appbackend.repository.InterviewConversationRepository;
import com.example.appbackend.repository.InterviewUserProfileRepository;
import com.example.appbackend.util.InterviewJsonHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class InterviewLangGraphService {

    private final InterviewConversationRepository conversationRepository;
    private final AiInterviewMessageRepository messageRepository;
    private final InterviewConfigRepository configRepository;
    private final InterviewUserProfileRepository profileRepository;
    private final LlmService llmService;
    private final ObjectMapper objectMapper;

    public InterviewLangGraphService(InterviewConversationRepository conversationRepository,
                                     AiInterviewMessageRepository messageRepository,
                                     InterviewConfigRepository configRepository,
                                     InterviewUserProfileRepository profileRepository,
                                     LlmService llmService,
                                     ObjectMapper objectMapper) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.configRepository = configRepository;
        this.profileRepository = profileRepository;
        this.llmService = llmService;
        this.objectMapper = objectMapper;
    }

    public SseEmitter chat(Map<String, Object> body, String authorization) {
        String conversationId = InterviewJsonHelper.asStr(body.get("conversation_id"));
        String content = InterviewJsonHelper.asStr(body.get("content"));
        if (!StringUtils.hasText(conversationId) || !StringUtils.hasText(content)) {
            throw new InterviewServiceException("missing_or_empty_field", 400);
        }
        InterviewConversation conversation = conversationRepository.findByConversationId(conversationId)
                .orElseThrow(() -> new InterviewServiceException("conversation_not_found", 400));

        persistMessage(conversation, "user", content, null);

        String jobRole = Optional.ofNullable(InterviewJsonHelper.asStr(body.get("job_role")))
                .filter(StringUtils::hasText)
                .orElse(Optional.ofNullable(conversation.getJobRole()).orElse("通用"));

        InterviewConfig cfg = configRepository.findFirstByConversationIdOrderByIdDesc(conversationId).orElse(null);
        String system = InterviewPrompts.systemText(jobRole);
        if (cfg != null) {
            String extra = InterviewPrompts.buildConfigContext(
                    cfg.getInterviewMode(), cfg.getInterviewerPersona(), cfg.getFocusTags(),
                    cfg.getRequirements(), cfg.getDifficulty());
            if (StringUtils.hasText(extra)) {
                system = system + "\n" + extra;
            }
        }

        StringBuilder history = new StringBuilder();
        List<AiInterviewMessage> messages = messageRepository.findByConversationIdOrderByIdAsc(conversationId);
        for (AiInterviewMessage m : messages) {
            history.append(m.getRole()).append(": ").append(m.getContent()).append('\n');
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> userInfo = body.get("user_info") instanceof Map<?, ?> ui
                ? (Map<String, Object>) ui : null;
        if (userInfo == null) {
            InterviewUserProfile profile = profileRepository.findByUserId(conversation.getUserId()).orElse(null);
            if (profile != null) {
                userInfo = new LinkedHashMap<>();
                userInfo.put("nickname", profile.getNickname());
                userInfo.put("education", profile.getEducation());
                userInfo.put("school", profile.getSchool());
                userInfo.put("major", profile.getMajor());
                userInfo.put("work_experience_years", profile.getWorkExperienceYears());
                userInfo.put("skill_tags", profile.getSkillTags());
                userInfo.put("target_position", profile.getTargetPosition());
            }
        }

        StringBuilder input = new StringBuilder();
        input.append("近七天记录：无\n");
        input.append("近期对话记忆：\n").append(history.isEmpty() ? "（面试开始）" : history).append('\n');
        if (userInfo != null) {
            input.append("候选人信息：").append(userInfo).append('\n');
        }
        input.append("候选人：").append(content).append('\n').append("面试官（安然）：");

        LlmChatRequest req = new LlmChatRequest();
        req.setPrompt(system);
        req.setInput(input.toString());

        SseEmitter emitter = new SseEmitter(0L);
        CompletableFuture.runAsync(() -> {
            try {
                LlmChatResponse resp = llmService.chat(req, authorization);
                String answer = resp != null && StringUtils.hasText(resp.getAnswer())
                        ? resp.getAnswer() : "好的，我们继续。请先做一个简短的自我介绍。";
                emitContentChunks(emitter, answer);
                persistMessage(conversation, "assistant", answer, null);
                emitter.send(SseEmitter.event().data("[DONE]"));
                emitter.complete();
            } catch (Exception e) {
                try {
                    Map<String, Object> err = Map.of("error", e.getMessage() == null ? "internal_error" : e.getMessage());
                    emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(err)));
                    emitter.send(SseEmitter.event().data("[DONE]"));
                } catch (Exception ignored) {
                }
                emitter.complete();
            }
        });
        return emitter;
    }

    private void emitContentChunks(SseEmitter emitter, String answer) throws IOException {
        int size = 24;
        for (int i = 0; i < answer.length(); i += size) {
            String chunk = answer.substring(i, Math.min(i + size, answer.length()));
            Map<String, Object> payload = Map.of("content", chunk);
            emitter.send(SseEmitter.event()
                    .data(objectMapper.writeValueAsString(payload), MediaType.APPLICATION_JSON));
        }
    }

    @Transactional
    protected void persistMessage(InterviewConversation conversation, String role, String content, String emotion) {
        AiInterviewMessage msg = new AiInterviewMessage();
        msg.setConversationPk(conversation.getId());
        msg.setConversationId(conversation.getConversationId());
        msg.setRole(role);
        msg.setContent(content);
        msg.setEmotionLabel(emotion);
        messageRepository.save(msg);
    }
}
