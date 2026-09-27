package com.example.appbackend.service;

import com.example.appbackend.dto.LlmChatRequest;
import com.example.appbackend.dto.LlmChatResponse;
import com.example.appbackend.entity.*;
import com.example.appbackend.exception.InterviewServiceException;
import com.example.appbackend.prompt.InterviewPrompts;
import com.example.appbackend.repository.*;
import com.example.appbackend.util.InterviewJsonHelper;
import com.example.appbackend.util.InterviewMaps;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class InterviewKnowledgeService {

    private static final Set<String> LEVELS = Set.of("初级", "中级", "高级");

    private final InterviewKnowledgeRepository knowledgeRepository;
    private final UserQuestionAttemptRepository attemptRepository;
    private final UserQuestionStatRepository statRepository;
    private final InterviewUserProfileRepository profileRepository;
    private final LlmService llmService;
    private final ChromaInterviewService chromaInterviewService;

    public InterviewKnowledgeService(InterviewKnowledgeRepository knowledgeRepository,
                                     UserQuestionAttemptRepository attemptRepository,
                                     UserQuestionStatRepository statRepository,
                                     InterviewUserProfileRepository profileRepository,
                                     LlmService llmService,
                                     ChromaInterviewService chromaInterviewService) {
        this.knowledgeRepository = knowledgeRepository;
        this.attemptRepository = attemptRepository;
        this.statRepository = statRepository;
        this.profileRepository = profileRepository;
        this.llmService = llmService;
        this.chromaInterviewService = chromaInterviewService;
    }

    public void requireManager(Map<String, Object> body, Long jwtUserId) {
        if (body != null) {
            Object isManager = body.get("is_manager");
            if (isManager != null && InterviewJsonHelper.asInt(isManager, 0) == 1) {
                return;
            }
            Object userIdObj = body.get("user_id");
            if (userIdObj != null) {
                Long uid = InterviewJsonHelper.asInt(userIdObj, null) == null
                        ? null
                        : Long.valueOf(InterviewJsonHelper.asInt(userIdObj, 0));
                if (uid != null && uid > 0) {
                    Optional<InterviewUserProfile> p = profileRepository.findByUserId(uid);
                    if (p.isPresent() && Objects.equals(p.get().getIsManager(), 1)) {
                        return;
                    }
                }
            }
        }
        if (jwtUserId != null && jwtUserId > 0) {
            Optional<InterviewUserProfile> p = profileRepository.findByUserId(jwtUserId);
            if (p.isPresent() && Objects.equals(p.get().getIsManager(), 1)) {
                return;
            }
        }
        throw new InterviewServiceException("forbidden", 400, Map.of("reason", "manager_required"));
    }

    @Transactional
    public Map<String, Object> create(Map<String, Object> body, Long jwtUserId) {
        requireManager(body, jwtUserId);
        InterviewKnowledge k = applyFields(new InterviewKnowledge(), body, true);
        k = knowledgeRepository.save(k);
        try {
            chromaInterviewService.upsertKnowledge(k);
        } catch (Exception ignored) {
        }
        return InterviewMaps.knowledgeItem(k);
    }

    public Map<String, Object> detail(Long id) {
        InterviewKnowledge k = knowledgeRepository.findById(id)
                .orElseThrow(() -> new InterviewServiceException("not_found", 404));
        return InterviewMaps.knowledgeItem(k);
    }

    @Transactional
    public Map<String, Object> update(Map<String, Object> body, Long jwtUserId) {
        requireManager(body, jwtUserId);
        Long id = body == null ? null : Long.valueOf(InterviewJsonHelper.asInt(body.get("id"), 0));
        if (id == null || id <= 0) {
            throw new InterviewServiceException("missing_or_empty_field", 400, Map.of("field", "id"));
        }
        InterviewKnowledge k = knowledgeRepository.findById(id)
                .orElseThrow(() -> new InterviewServiceException("not_found", 404));
        applyFields(k, body, false);
        k = knowledgeRepository.save(k);
        try {
            chromaInterviewService.upsertKnowledge(k);
        } catch (Exception ignored) {
        }
        return InterviewMaps.knowledgeItem(k);
    }

    @Transactional
    public Map<String, Object> delete(Long id, Map<String, Object> body, Long jwtUserId) {
        requireManager(body, jwtUserId);
        if (id == null || id <= 0) {
            throw new InterviewServiceException("missing_or_empty_field", 400, Map.of("field", "id"));
        }
        if (!knowledgeRepository.existsById(id)) {
            throw new InterviewServiceException("not_found", 404);
        }
        knowledgeRepository.deleteById(id);
        return Map.of("ok", true);
    }

    public Map<String, Object> list(String jobPosition, String questionType, String q, Integer status,
                                    int page, int pageSize, Long jwtUserId, Map<String, Object> filters) {
        // 题库列表对登录用户开放（与 Django 一致）；管理员校验仅用于 create/update/delete
        int p = Math.max(page, 1);
        int ps = Math.min(Math.max(pageSize, 1), 100);
        boolean adminView = filters != null && InterviewJsonHelper.asInt(filters.get("is_manager"), 0) == 1;
        Integer effectiveStatus = status;
        if (effectiveStatus == null && !adminView) {
            // 普通练习默认只看启用题；管理端不传 status 时看全部
            effectiveStatus = 1;
        }
        Page<InterviewKnowledge> result = knowledgeRepository.search(
                jobPosition, questionType, effectiveStatus, q, PageRequest.of(p - 1, ps));
        List<Map<String, Object>> items = result.getContent().stream().map(InterviewMaps::knowledgeItem).toList();
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("total", result.getTotalElements());
        resp.put("page", p);
        resp.put("page_size", ps);
        resp.put("items", items);
        return resp;
    }

    public Map<String, Object> types(String jobPosition, Integer status) {
        List<Map<String, Object>> items = knowledgeRepository.findDistinctQuestionTypes(jobPosition, status).stream()
                .map(t -> Map.<String, Object>of("label", t, "value", t))
                .toList();
        return Map.of("items", items);
    }

    public Map<String, Object> positions(String questionType, Integer status) {
        List<Map<String, Object>> items = knowledgeRepository.findDistinctJobPositions(questionType, status).stream()
                .map(t -> Map.<String, Object>of("label", t, "value", t))
                .toList();
        return Map.of("items", items);
    }

    public Map<String, Object> aiRecognize(Map<String, Object> body, String authorization) {
        String source = firstNonBlank(body, "source_text", "text", "content");
        if (!StringUtils.hasText(source)) {
            throw new InterviewServiceException("missing_or_empty_field", 400, Map.of("field", "source_text"));
        }
        String hintJob = InterviewJsonHelper.asStr(body.get("job_position"));
        String hintType = InterviewJsonHelper.asStr(body.get("question_type"));
        String userPrompt = "请识别以下面试题文本并抽取字段：\n" + source
                + (StringUtils.hasText(hintJob) ? "\n提示岗位：" + hintJob : "")
                + (StringUtils.hasText(hintType) ? "\n提示题型：" + hintType : "");
        try {
            LlmChatRequest req = new LlmChatRequest();
            req.setPrompt(InterviewPrompts.AI_RECOGNIZE_SYSTEM_PROMPT);
            req.setInput(userPrompt);
            LlmChatResponse resp = llmService.chat(req, authorization);
            Map<String, Object> parsed = InterviewJsonHelper.extractJsonObject(
                    resp != null ? resp.getAnswer() : null);
            Map<String, Object> out = new LinkedHashMap<>();
            String jobPosition = InterviewJsonHelper.asText(parsed.get("job_position"), hintJob != null ? hintJob : "");
            out.put("job_position", jobPosition);
            boolean exists = StringUtils.hasText(jobPosition)
                    && knowledgeRepository.findDistinctJobPositions(null, null).stream().anyMatch(jobPosition::equals);
            out.put("job_position_exists", exists);
            out.put("should_create_new_job_position", !exists);
            out.put("matched_existing_job_position", exists ? jobPosition : null);
            out.put("question_type", InterviewJsonHelper.asText(parsed.get("question_type"),
                    hintType != null ? hintType : ""));
            out.put("question", InterviewJsonHelper.asText(parsed.get("question"), ""));
            out.put("excellent_answer", InterviewJsonHelper.asText(parsed.get("excellent_answer"), ""));
            Integer difficultyObj = InterviewJsonHelper.asInt(parsed.get("difficulty"), 3);
            int difficulty = difficultyObj == null ? 3 : difficultyObj;
            out.put("difficulty", Math.min(5, Math.max(1, difficulty)));
            out.put("keywords", InterviewJsonHelper.asText(parsed.get("keywords"), ""));
            out.put("question_intent", InterviewJsonHelper.asText(parsed.get("question_intent"), ""));
            out.put("answer_points", InterviewJsonHelper.asText(parsed.get("answer_points"), ""));
            out.put("score_standard", InterviewJsonHelper.asText(parsed.get("score_standard"), ""));
            String level = InterviewJsonHelper.asText(parsed.get("suitable_level"), "中级");
            if (!LEVELS.contains(level)) level = "中级";
            out.put("suitable_level", level);
            out.put("remark", InterviewJsonHelper.asText(parsed.get("remark"), ""));
            out.put("_rules", Map.of(
                    "required_fields", List.of("job_position", "question_type", "question"),
                    "difficulty", "1-5 integer",
                    "suitable_level", "enum: 初级 | 中级 | 高级",
                    "excluded_fields", List.of("id", "status", "created_at", "updated_at")
            ));
            return out;
        } catch (InterviewServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new InterviewServiceException("ai_recognize_failed", 500);
        }
    }

    @Transactional
    public Map<String, Object> grade(Map<String, Object> body, Long userId, String authorization) {
        if (userId == null || userId <= 0) {
            throw new InterviewServiceException("invalid_or_expired_session", 401);
        }
        Long id = Long.valueOf(InterviewJsonHelper.asInt(body.get("id"), 0));
        String userAnswer = InterviewJsonHelper.asStr(body.get("user_answer"));
        if (id == null || id <= 0) {
            throw new InterviewServiceException("missing_or_empty_field", 400, Map.of("field", "id"));
        }
        if (!StringUtils.hasText(userAnswer)) {
            throw new InterviewServiceException("missing_or_empty_field", 400, Map.of("field", "user_answer"));
        }
        InterviewKnowledge k = knowledgeRepository.findById(id)
                .orElseThrow(() -> new InterviewServiceException("not_found", 404));

        Map<String, Object> grade;
        try {
            String prompt = "题目：\n" + k.getQuestion()
                    + "\n\n候选人答案：\n" + userAnswer
                    + "\n\n优秀答案示例：\n" + nullToDash(k.getExcellentAnswer())
                    + "\n\n答题要点：\n" + nullToDash(k.getAnswerPoints())
                    + "\n\n评分标准：\n" + nullToDash(k.getScoreStandard())
                    + "\n\n考察意图：\n" + nullToDash(k.getQuestionIntent())
                    + "\n\n请根据题目与答案给出评分与反馈。";
            LlmChatRequest req = new LlmChatRequest();
            req.setPrompt(InterviewPrompts.GRADE_SYSTEM_PROMPT);
            req.setInput(prompt);
            LlmChatResponse resp = llmService.chat(req, authorization);
            Map<String, Object> parsed = InterviewJsonHelper.extractJsonObject(
                    resp != null ? resp.getAnswer() : null);
            grade = new LinkedHashMap<>();
            grade.put("score", InterviewJsonHelper.toScore(parsed.get("score")));
            grade.put("reference_answer", InterviewJsonHelper.asText(parsed.get("reference_answer"),
                    k.getExcellentAnswer() != null ? k.getExcellentAnswer() : ""));
            grade.put("strengths", InterviewJsonHelper.asText(parsed.get("strengths"), ""));
            grade.put("weaknesses", InterviewJsonHelper.asText(parsed.get("weaknesses"), ""));
            grade.put("improvement_suggestions", InterviewJsonHelper.asText(parsed.get("improvement_suggestions"), ""));
            grade.put("overall_comment", InterviewJsonHelper.asText(parsed.get("overall_comment"), ""));
        } catch (Exception e) {
            throw new InterviewServiceException("ai_grade_failed", 500);
        }

        int score = InterviewJsonHelper.toScore(grade.get("score"));
        int isWrong = score < 60 ? 1 : 0;
        try {
            UserQuestionAttempt attempt = new UserQuestionAttempt();
            attempt.setUserId(userId);
            attempt.setKnowledgeId(k.getId());
            attempt.setUserAnswer(userAnswer);
            attempt.setAiScore(score);
            attempt.setIsWrong(isWrong);
            attempt.setReferenceAnswer(InterviewJsonHelper.asStr(grade.get("reference_answer")));
            attempt.setStrengths(InterviewJsonHelper.asStr(grade.get("strengths")));
            attempt.setWeaknesses(InterviewJsonHelper.asStr(grade.get("weaknesses")));
            attempt.setImprovementSuggestions(InterviewJsonHelper.asStr(grade.get("improvement_suggestions")));
            attempt.setOverallComment(InterviewJsonHelper.asStr(grade.get("overall_comment")));
            attempt = attemptRepository.save(attempt);

            UserQuestionStat stat = statRepository.findByUserIdAndKnowledgeId(userId, k.getId())
                    .orElseGet(() -> {
                        UserQuestionStat s = new UserQuestionStat();
                        s.setUserId(userId);
                        s.setKnowledgeId(k.getId());
                        s.setFirstAttemptAt(LocalDateTime.now());
                        return s;
                    });
            int total = Optional.ofNullable(stat.getTotalAttempts()).orElse(0) + 1;
            int wrong = Optional.ofNullable(stat.getWrongAttempts()).orElse(0) + (isWrong == 1 ? 1 : 0);
            BigDecimal prevSum = Optional.ofNullable(stat.getAvgScore()).orElse(BigDecimal.ZERO)
                    .multiply(BigDecimal.valueOf(Math.max(total - 1, 0)));
            BigDecimal avg = prevSum.add(BigDecimal.valueOf(score))
                    .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
            stat.setTotalAttempts(total);
            stat.setWrongAttempts(wrong);
            stat.setLatestScore(score);
            stat.setBestScore(Math.max(Optional.ofNullable(stat.getBestScore()).orElse(0), score));
            stat.setAvgScore(avg);
            stat.setIsWrongBook(1);
            stat.setWrongStreak(isWrong == 1
                    ? Optional.ofNullable(stat.getWrongStreak()).orElse(0) + 1
                    : 0);
            stat.setLastAttemptAt(LocalDateTime.now());
            if (stat.getFirstAttemptAt() == null) {
                stat.setFirstAttemptAt(LocalDateTime.now());
            }
            stat = statRepository.save(stat);

            Map<String, Object> out = new LinkedHashMap<>();
            out.put("id", k.getId());
            out.put("question", k.getQuestion());
            out.put("attempt_id", attempt.getId());
            out.put("is_wrong_book", stat.getIsWrongBook());
            out.put("wrong_streak", stat.getWrongStreak());
            out.put("score", score);
            out.put("reference_answer", grade.get("reference_answer"));
            out.put("strengths", grade.get("strengths"));
            out.put("weaknesses", grade.get("weaknesses"));
            out.put("improvement_suggestions", grade.get("improvement_suggestions"));
            out.put("overall_comment", grade.get("overall_comment"));
            return out;
        } catch (InterviewServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new InterviewServiceException("save_grade_result_failed", 500);
        }
    }

    private InterviewKnowledge applyFields(InterviewKnowledge k, Map<String, Object> body, boolean creating) {
        String jobPosition = InterviewJsonHelper.asStr(body.get("job_position"));
        String questionType = InterviewJsonHelper.asStr(body.get("question_type"));
        String question = InterviewJsonHelper.asStr(body.get("question"));
        if (creating) {
            if (!StringUtils.hasText(jobPosition)) {
                throw new InterviewServiceException("missing_or_empty_field", 400, Map.of("field", "job_position"));
            }
            if (!StringUtils.hasText(questionType)) {
                throw new InterviewServiceException("missing_or_empty_field", 400, Map.of("field", "question_type"));
            }
            if (!StringUtils.hasText(question)) {
                throw new InterviewServiceException("missing_or_empty_field", 400, Map.of("field", "question"));
            }
        }
        if (body.containsKey("job_position") || creating) k.setJobPosition(jobPosition);
        if (body.containsKey("question_type") || creating) k.setQuestionType(questionType);
        if (body.containsKey("question") || creating) k.setQuestion(question);
        if (body.containsKey("excellent_answer")) k.setExcellentAnswer(InterviewJsonHelper.asStr(body.get("excellent_answer")));
        if (body.containsKey("keywords")) k.setKeywords(InterviewJsonHelper.asStr(body.get("keywords")));
        if (body.containsKey("question_intent")) k.setQuestionIntent(InterviewJsonHelper.asStr(body.get("question_intent")));
        if (body.containsKey("answer_points")) k.setAnswerPoints(InterviewJsonHelper.asStr(body.get("answer_points")));
        if (body.containsKey("score_standard")) k.setScoreStandard(InterviewJsonHelper.asStr(body.get("score_standard")));
        if (body.containsKey("remark")) k.setRemark(InterviewJsonHelper.asStr(body.get("remark")));
        if (body.containsKey("difficulty") || creating) {
            Integer d = InterviewJsonHelper.asInt(body.get("difficulty"), 3);
            if (d == null || d < 1 || d > 5) {
                throw new InterviewServiceException("invalid_difficulty", 400);
            }
            k.setDifficulty(d);
        }
        if (body.containsKey("status") || creating) {
            Integer s = InterviewJsonHelper.asInt(body.get("status"), 1);
            if (s == null || (s != 0 && s != 1)) {
                throw new InterviewServiceException("invalid_status", 400);
            }
            k.setStatus(s);
        }
        if (body.containsKey("suitable_level")) {
            String level = InterviewJsonHelper.asStr(body.get("suitable_level"));
            if (StringUtils.hasText(level) && !LEVELS.contains(level)) {
                throw new InterviewServiceException("invalid_suitable_level", 400);
            }
            k.setSuitableLevel(level);
        }
        return k;
    }

    private static String firstNonBlank(Map<String, Object> body, String... keys) {
        if (body == null) return null;
        for (String key : keys) {
            String v = InterviewJsonHelper.asStr(body.get(key));
            if (StringUtils.hasText(v)) return v.trim();
        }
        return null;
    }

    private static String nullToDash(String v) {
        return StringUtils.hasText(v) ? v : "-";
    }
}
