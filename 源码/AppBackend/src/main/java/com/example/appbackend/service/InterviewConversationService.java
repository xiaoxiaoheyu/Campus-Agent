package com.example.appbackend.service;

import com.example.appbackend.entity.InterviewConfig;
import com.example.appbackend.entity.InterviewConversation;
import com.example.appbackend.entity.InterviewEvaluation;
import com.example.appbackend.entity.InterviewUserProfile;
import com.example.appbackend.exception.InterviewServiceException;
import com.example.appbackend.repository.InterviewConfigRepository;
import com.example.appbackend.repository.InterviewConversationRepository;
import com.example.appbackend.repository.InterviewEvaluationRepository;
import com.example.appbackend.repository.InterviewUserProfileRepository;
import com.example.appbackend.util.InterviewJsonHelper;
import com.example.appbackend.util.InterviewMaps;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class InterviewConversationService {

    private final InterviewConversationRepository conversationRepository;
    private final InterviewConfigRepository configRepository;
    private final InterviewEvaluationRepository evaluationRepository;
    private final InterviewUserProfileRepository profileRepository;

    public InterviewConversationService(InterviewConversationRepository conversationRepository,
                                        InterviewConfigRepository configRepository,
                                        InterviewEvaluationRepository evaluationRepository,
                                        InterviewUserProfileRepository profileRepository) {
        this.conversationRepository = conversationRepository;
        this.configRepository = configRepository;
        this.evaluationRepository = evaluationRepository;
        this.profileRepository = profileRepository;
    }

    @Transactional
    public Map<String, Object> newId(Map<String, Object> body, Long jwtUserId) {
        Long userId = jwtUserId;
        if (body != null && body.get("user_id") != null) {
            Integer uid = InterviewJsonHelper.asInt(body.get("user_id"), null);
            if (uid != null && uid > 0) userId = uid.longValue();
        }
        if (userId == null || userId <= 0) {
            throw new InterviewServiceException("missing_or_empty_field", 400, Map.of("field", "user_id_or_session_token"));
        }
        String conversationId = body == null ? null : InterviewJsonHelper.asStr(body.get("conversation_id"));
        if (!StringUtils.hasText(conversationId)) {
            conversationId = UUID.randomUUID().toString();
        }
        Optional<InterviewConversation> existing = conversationRepository.findByConversationId(conversationId);
        if (existing.isPresent()) {
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("conversation_id", existing.get().getConversationId());
            resp.put("created_at", InterviewMaps.iso(existing.get().getCreatedAt()) + "+00:00");
            return resp;
        }
        InterviewConversation c = new InterviewConversation();
        c.setConversationId(conversationId);
        c.setUserId(userId);
        c.setJobRole(body == null ? null : InterviewJsonHelper.asStr(body.get("job_role")));
        c.setStatus("created");
        c = conversationRepository.save(c);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("conversation_id", c.getConversationId());
        resp.put("created_at", InterviewMaps.iso(c.getCreatedAt()) + "+00:00");
        return resp;
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public Map<String, Object> start(Map<String, Object> body) {
        String conversationId = InterviewJsonHelper.asStr(body.get("conversation_id"));
        if (!StringUtils.hasText(conversationId)) {
            throw new InterviewServiceException("missing_or_empty_field", 400, Map.of("field", "conversation_id"));
        }
        InterviewConversation c = conversationRepository.findByConversationId(conversationId)
                .orElseThrow(() -> new InterviewServiceException("not_found", 400));
        c.setStatus("running");
        if (c.getStartedAt() == null) {
            c.setStartedAt(LocalDateTime.now());
        }
        c = conversationRepository.save(c);

        Map<String, Object> configBody = null;
        if (body.get("config") instanceof Map<?, ?> nested) {
            configBody = (Map<String, Object>) nested;
        } else if (body.containsKey("target_position")) {
            configBody = body;
        }
        InterviewConfig createdConfig = null;
        if (configBody != null) {
            String target = InterviewJsonHelper.asStr(configBody.get("target_position"));
            if (!StringUtils.hasText(target)) {
                throw new InterviewServiceException("missing_or_empty_field", 400, Map.of("field", "target_position"));
            }
            InterviewConfig cfg = new InterviewConfig();
            cfg.setConversationPk(c.getId());
            cfg.setConversationId(c.getConversationId());
            cfg.setTargetPosition(target);
            cfg.setInterviewMode(Optional.ofNullable(InterviewJsonHelper.asStr(configBody.get("interview_mode"))).orElse("full"));
            cfg.setInterviewerPersona(Optional.ofNullable(InterviewJsonHelper.asStr(configBody.get("interviewer_persona"))).orElse("neutral"));
            cfg.setFocusTags(InterviewJsonHelper.asStr(configBody.get("focus_tags")));
            cfg.setRequirements(InterviewJsonHelper.asStr(configBody.get("requirements")));
            cfg.setDifficulty(InterviewJsonHelper.asInt(configBody.get("difficulty"), 3));
            cfg.setRemark(InterviewJsonHelper.asStr(configBody.get("remark")));
            cfg.setStatus(InterviewJsonHelper.asInt(configBody.get("status"), 1));
            createdConfig = configRepository.save(cfg);
            if (StringUtils.hasText(target) && !StringUtils.hasText(c.getJobRole())) {
                c.setJobRole(target);
                c = conversationRepository.save(c);
            }
        }
        Map<String, Object> item = toConversationItem(c);
        if (createdConfig != null) {
            item.put("config", InterviewMaps.configItem(createdConfig));
        }
        return item;
    }

    @Transactional
    public Map<String, Object> end(Map<String, Object> body) {
        String conversationId = InterviewJsonHelper.asStr(body.get("conversation_id"));
        InterviewConversation c = conversationRepository.findByConversationId(conversationId)
                .orElseThrow(() -> new InterviewServiceException("not_found", 400));
        c.setStatus("finished");
        c.setEndedAt(LocalDateTime.now());
        c = conversationRepository.save(c);
        return toConversationItem(c);
    }

    @Transactional
    public Map<String, Object> commit(Map<String, Object> body) {
        String conversationId = InterviewJsonHelper.asStr(body.get("conversation_id"));
        InterviewConversation c = conversationRepository.findByConversationId(conversationId)
                .orElseThrow(() -> new InterviewServiceException("not_found", 400));
        boolean firstFinish = !"finished".equals(c.getStatus());
        if (body.get("user_id") != null) {
            Integer uid = InterviewJsonHelper.asInt(body.get("user_id"), null);
            if (uid != null && uid > 0) c.setUserId(uid.longValue());
        }
        if (body.containsKey("job_role")) c.setJobRole(InterviewJsonHelper.asStr(body.get("job_role")));
        c.setStartedAt(parseTime(InterviewJsonHelper.asStr(body.get("started_at")), c.getStartedAt()));
        c.setEndedAt(parseTime(InterviewJsonHelper.asStr(body.get("ended_at")), LocalDateTime.now()));
        c.setStatus("finished");
        c = conversationRepository.save(c);
        if (firstFinish) {
            profileRepository.findByUserId(c.getUserId()).ifPresent(p -> {
                p.setInterviewCount(Optional.ofNullable(p.getInterviewCount()).orElse(0) + 1);
                profileRepository.save(p);
            });
        }
        return toConversationItem(c);
    }

    public Map<String, Object> list(Long userId, Integer page, Integer pageSize, String status, String jobRole, String orderBy) {
        int p = page == null || page < 1 ? 1 : page;
        int ps = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        if ("started_at".equals(orderBy)) sort = Sort.by(Sort.Direction.DESC, "startedAt");
        Page<InterviewConversation> result = conversationRepository.search(userId, status, jobRole, PageRequest.of(p - 1, ps, sort));
        List<Map<String, Object>> items = result.getContent().stream().map(this::toConversationItem).toList();
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("total", result.getTotalElements());
        resp.put("page", p);
        resp.put("page_size", ps);
        resp.put("items", items);
        return resp;
    }

    public Map<String, Object> history(String conversationId) {
        // implemented via message service in controller; keep bridge here if needed
        throw new UnsupportedOperationException();
    }

    private Map<String, Object> toConversationItem(InterviewConversation c) {
        Optional<InterviewEvaluation> eval = evaluationRepository.findFirstByConversationIdOrderByIdDesc(c.getConversationId());
        return InterviewMaps.conversationItem(
                c,
                eval.isPresent(),
                eval.map(InterviewEvaluation::getId).orElse(null),
                eval.map(InterviewEvaluation::getScore).orElse(null),
                eval.map(InterviewEvaluation::getCreatedAt).orElse(null)
        );
    }

    private LocalDateTime parseTime(String raw, LocalDateTime fallback) {
        if (!StringUtils.hasText(raw)) return fallback;
        try {
            String cleaned = raw.replace("Z", "").replace("+00:00", "");
            if (cleaned.length() > 19) cleaned = cleaned.substring(0, 19);
            return LocalDateTime.parse(cleaned);
        } catch (DateTimeParseException e) {
            return fallback;
        }
    }
}
