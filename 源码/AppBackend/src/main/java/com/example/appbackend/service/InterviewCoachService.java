package com.example.appbackend.service;

import com.example.appbackend.dto.LlmChatRequest;
import com.example.appbackend.dto.LlmChatResponse;
import com.example.appbackend.entity.InterviewUserProfile;
import com.example.appbackend.exception.InterviewServiceException;
import com.example.appbackend.prompt.InterviewPrompts;
import com.example.appbackend.repository.InterviewUserProfileRepository;
import com.example.appbackend.util.InterviewJsonHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class InterviewCoachService {

    private final InterviewUserProfileRepository profileRepository;
    private final LlmService llmService;

    public InterviewCoachService(InterviewUserProfileRepository profileRepository, LlmService llmService) {
        this.profileRepository = profileRepository;
        this.llmService = llmService;
    }

    public SseEmitter coach(Long userId, Map<String, Object> body, String authorization) {
        String message = InterviewJsonHelper.asStr(body.get("message"));
        if (!StringUtils.hasText(message)) {
            throw new InterviewServiceException("missing_or_empty_field", 400, Map.of("field", "message"));
        }
        InterviewUserProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new InterviewServiceException("user_not_found", 404));

        Map<String, Object> ctx = new java.util.LinkedHashMap<>();
        ctx.put("nickname", Optional.ofNullable(profile.getNickname()).orElse(""));
        ctx.put("target_position", Optional.ofNullable(profile.getTargetPosition()).orElse(""));
        ctx.put("skill_tags", Optional.ofNullable(profile.getSkillTags()).orElse(""));
        ctx.put("tech_stack", Optional.ofNullable(profile.getTechStack()).orElse(""));
        ctx.put("work_experience_years", Optional.ofNullable(profile.getWorkExperienceYears()).orElse(0));
        ctx.put("education", Optional.ofNullable(profile.getEducation()).orElse(""));
        ctx.put("major", Optional.ofNullable(profile.getMajor()).orElse(""));
        ctx.put("school", Optional.ofNullable(profile.getSchool()).orElse(""));
        String prompt = InterviewPrompts.buildCoachSystemPrompt(ctx);

        return streamPlain(prompt, message, authorization);
    }

    public SseEmitter customPath(Long userId, Map<String, Object> body, String authorization) {
        String action = InterviewJsonHelper.asStr(body.get("action"));
        if (!StringUtils.hasText(action)) {
            throw new InterviewServiceException("invalid_action", 400);
        }
        String message = InterviewJsonHelper.asStr(body.get("message"));
        String resume = InterviewJsonHelper.asStr(body.get("resume_content"));
        if ("optimize_resume".equals(action) && !StringUtils.hasText(resume)) {
            throw new InterviewServiceException("missing_or_empty_field", 400, Map.of("field", "resume_content"));
        }
        InterviewUserProfile profile = profileRepository.findByUserId(userId).orElse(null);
        String system = switch (action) {
            case "career_path" -> "你是职业路径规划专家，请输出可执行的学习与求职路径。";
            case "generate_resume" -> "你是简历生成专家，请根据用户信息生成专业简历草稿。";
            case "optimize_resume" -> "你是简历优化专家，请优化用户提供的简历内容。";
            default -> throw new InterviewServiceException("invalid_action", 400);
        };
        String input = "用户消息：" + Optional.ofNullable(message).orElse("")
                + "\n画像：" + (profile == null ? "{}" : profile.getTargetPosition() + "/" + profile.getSkillTags())
                + ("optimize_resume".equals(action) ? "\n原简历：\n" + resume : "");
        return streamPlain(system, input, authorization);
    }

    private SseEmitter streamPlain(String system, String input, String authorization) {
        SseEmitter emitter = new SseEmitter(0L);
        CompletableFuture.runAsync(() -> {
            try {
                LlmChatRequest req = new LlmChatRequest();
                req.setPrompt(system);
                req.setInput(input);
                LlmChatResponse resp = llmService.chat(req, authorization);
                String answer = resp != null && StringUtils.hasText(resp.getAnswer())
                        ? resp.getAnswer() : "暂时无法生成建议，请稍后再试。";
                int size = 40;
                for (int i = 0; i < answer.length(); i += size) {
                    String chunk = answer.substring(i, Math.min(i + size, answer.length()));
                    emitter.send(SseEmitter.event().data(chunk));
                }
                emitter.send(SseEmitter.event().data("[DONE]"));
                emitter.complete();
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().data("ERROR:" + e.getMessage()));
                    emitter.send(SseEmitter.event().data("[DONE]"));
                } catch (Exception ignored) {
                }
                emitter.complete();
            }
        });
        return emitter;
    }
}
