package com.example.appbackend.service;

import com.example.appbackend.entity.InterviewConfig;
import com.example.appbackend.exception.InterviewServiceException;
import com.example.appbackend.repository.InterviewConfigRepository;
import com.example.appbackend.util.InterviewJsonHelper;
import com.example.appbackend.util.InterviewMaps;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Map;

@Service
public class InterviewConfigApiService {

    private final InterviewConfigRepository configRepository;

    public InterviewConfigApiService(InterviewConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    public Map<String, Object> get(Long id, String conversationId) {
        InterviewConfig cfg;
        if (id != null && id > 0) {
            cfg = configRepository.findById(id)
                    .orElseThrow(() -> new InterviewServiceException("not_found", 404));
        } else if (StringUtils.hasText(conversationId)) {
            cfg = configRepository.findFirstByConversationIdOrderByIdDesc(conversationId)
                    .orElseThrow(() -> new InterviewServiceException("not_found", 404));
        } else {
            throw new InterviewServiceException("missing_or_empty_field", 400, Map.of("field", "id_or_conversation_id"));
        }
        return InterviewMaps.configItem(cfg);
    }

    @Transactional
    public Map<String, Object> update(Map<String, Object> body) {
        Long id = Long.valueOf(InterviewJsonHelper.asInt(body.get("id"), 0));
        if (id == null || id <= 0) {
            throw new InterviewServiceException("missing_or_empty_field", 400, Map.of("field", "id"));
        }
        InterviewConfig cfg = configRepository.findById(id)
                .orElseThrow(() -> new InterviewServiceException("not_found", 404));
        if (body.containsKey("target_position")) cfg.setTargetPosition(InterviewJsonHelper.asStr(body.get("target_position")));
        if (body.containsKey("interview_mode")) cfg.setInterviewMode(InterviewJsonHelper.asStr(body.get("interview_mode")));
        if (body.containsKey("interviewer_persona")) cfg.setInterviewerPersona(InterviewJsonHelper.asStr(body.get("interviewer_persona")));
        if (body.containsKey("focus_tags")) cfg.setFocusTags(InterviewJsonHelper.asStr(body.get("focus_tags")));
        if (body.containsKey("requirements")) cfg.setRequirements(InterviewJsonHelper.asStr(body.get("requirements")));
        if (body.containsKey("difficulty")) cfg.setDifficulty(InterviewJsonHelper.asInt(body.get("difficulty"), cfg.getDifficulty()));
        if (body.containsKey("remark")) cfg.setRemark(InterviewJsonHelper.asStr(body.get("remark")));
        if (body.containsKey("status")) cfg.setStatus(InterviewJsonHelper.asInt(body.get("status"), cfg.getStatus()));
        cfg = configRepository.save(cfg);
        return InterviewMaps.configItem(cfg);
    }
}
