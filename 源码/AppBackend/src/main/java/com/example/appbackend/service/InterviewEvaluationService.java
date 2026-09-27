package com.example.appbackend.service;

import com.example.appbackend.dto.LlmChatRequest;
import com.example.appbackend.dto.LlmChatResponse;
import com.example.appbackend.entity.*;
import com.example.appbackend.exception.InterviewServiceException;
import com.example.appbackend.prompt.InterviewPrompts;
import com.example.appbackend.repository.*;
import com.example.appbackend.util.InterviewJsonHelper;
import com.example.appbackend.util.InterviewMaps;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class InterviewEvaluationService {

    private final InterviewEvaluationRepository evaluationRepository;
    private final InterviewConversationRepository conversationRepository;
    private final AiInterviewMessageRepository messageRepository;
    private final InterviewUserProfileRepository profileRepository;
    private final LlmService llmService;

    public InterviewEvaluationService(InterviewEvaluationRepository evaluationRepository,
                                      InterviewConversationRepository conversationRepository,
                                      AiInterviewMessageRepository messageRepository,
                                      InterviewUserProfileRepository profileRepository,
                                      LlmService llmService) {
        this.evaluationRepository = evaluationRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.profileRepository = profileRepository;
        this.llmService = llmService;
    }

    @Transactional
    public Map<String, Object> summarize(String conversationId, String authorization) {
        if (!StringUtils.hasText(conversationId)) {
            throw new InterviewServiceException("missing_or_empty_field", 400, Map.of("field", "conversation_id"));
        }
        Optional<InterviewEvaluation> existing = evaluationRepository.findFirstByConversationIdOrderByIdDesc(conversationId);
        if (existing.isPresent()) {
            return InterviewMaps.evaluationItem(existing.get());
        }
        InterviewConversation conversation = conversationRepository.findByConversationId(conversationId)
                .orElseThrow(() -> new InterviewServiceException("conversation_not_found", 404));

        InterviewUserProfile profile = profileRepository.findByUserId(conversation.getUserId()).orElse(null);
        List<AiInterviewMessage> messages = messageRepository.findByConversationIdOrderByIdAsc(conversationId);
        StringBuilder history = new StringBuilder();
        for (AiInterviewMessage m : messages) {
            history.append(m.getRole()).append(": ").append(m.getContent()).append('\n');
        }

        StringBuilder context = new StringBuilder();
        context.append("岗位：").append(Optional.ofNullable(conversation.getJobRole()).orElse("通用")).append('\n');
        if (profile != null) {
            context.append("候选人：")
                    .append(Optional.ofNullable(profile.getNickname()).orElse("未知"))
                    .append("，学历：").append(Optional.ofNullable(profile.getEducation()).orElse("未知"))
                    .append("，学校：").append(Optional.ofNullable(profile.getSchool()).orElse("未知"))
                    .append("，专业：").append(Optional.ofNullable(profile.getMajor()).orElse("未知"))
                    .append("，经验：").append(Optional.ofNullable(profile.getWorkExperienceYears()).orElse(0)).append("年")
                    .append("，技能：").append(Optional.ofNullable(profile.getSkillTags()).orElse("无"))
                    .append('\n');
        }
        context.append("对话记录：\n").append(history.isEmpty() ? "（无历史）" : history);

        try {
            LlmChatRequest req = new LlmChatRequest();
            req.setPrompt(InterviewPrompts.EVALUATION_SYSTEM_PROMPT);
            req.setInput(context.toString());
            LlmChatResponse resp = llmService.chat(req, authorization);
            Map<String, Object> parsed = InterviewJsonHelper.extractJsonObject(resp != null ? resp.getAnswer() : null);

            InterviewEvaluation evaluation = new InterviewEvaluation();
            evaluation.setConversationId(conversationId);
            evaluation.setUserId(conversation.getUserId());
            evaluation.setJobRole(conversation.getJobRole());
            evaluation.setScore(InterviewJsonHelper.toScore(parsed.get("score")));
            evaluation.setCoreConclusion(InterviewJsonHelper.asText(parsed.get("core_conclusion"), "暂无核心结论"));
            evaluation.setStrengths(InterviewJsonHelper.asText(parsed.get("strengths"), ""));
            evaluation.setWeaknesses(InterviewJsonHelper.asText(parsed.get("weaknesses"), ""));
            evaluation.setImprovements(InterviewJsonHelper.asText(parsed.get("improvements"), ""));
            evaluation = evaluationRepository.save(evaluation);

            if (profile != null) {
                int reportCount = Optional.ofNullable(profile.getAiReportCount()).orElse(0) + 1;
                profile.setAiReportCount(reportCount);
                List<InterviewEvaluation> all = evaluationRepository.findByUserIdOrderByIdDesc(profile.getUserId());
                double avg = all.stream().mapToInt(e -> Optional.ofNullable(e.getScore()).orElse(0)).average().orElse(0);
                profile.setAverageScore(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
                profileRepository.save(profile);
            }
            return InterviewMaps.evaluationItem(evaluation);
        } catch (InterviewServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new InterviewServiceException("internal_error", 500);
        }
    }
}
