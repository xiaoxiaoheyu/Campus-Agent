package com.example.appbackend.service;

import com.example.appbackend.entity.InterviewKnowledge;
import com.example.appbackend.entity.UserQuestionCollection;
import com.example.appbackend.entity.UserQuestionAttempt;
import com.example.appbackend.entity.UserQuestionStat;
import com.example.appbackend.exception.InterviewServiceException;
import com.example.appbackend.repository.InterviewKnowledgeRepository;
import com.example.appbackend.repository.UserQuestionAttemptRepository;
import com.example.appbackend.repository.UserQuestionCollectionRepository;
import com.example.appbackend.repository.UserQuestionStatRepository;
import com.example.appbackend.util.InterviewJsonHelper;
import com.example.appbackend.util.InterviewMaps;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class InterviewCollectionService {

    private final UserQuestionCollectionRepository collectionRepository;
    private final InterviewKnowledgeRepository knowledgeRepository;
    private final UserQuestionStatRepository statRepository;
    private final UserQuestionAttemptRepository attemptRepository;

    public InterviewCollectionService(UserQuestionCollectionRepository collectionRepository,
                                      InterviewKnowledgeRepository knowledgeRepository,
                                      UserQuestionStatRepository statRepository,
                                      UserQuestionAttemptRepository attemptRepository) {
        this.collectionRepository = collectionRepository;
        this.knowledgeRepository = knowledgeRepository;
        this.statRepository = statRepository;
        this.attemptRepository = attemptRepository;
    }

    public Map<String, Object> list(Long userId, String q, String jobPosition, Long knowledgeId) {
        List<UserQuestionCollection> rows = collectionRepository.findByUserIdOrderByCreatedAtDesc(userId);
        Map<Long, InterviewKnowledge> knowledgeMap = knowledgeRepository.findAllById(
                rows.stream().map(UserQuestionCollection::getKnowledgeId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(InterviewKnowledge::getId, x -> x));

        List<Map<String, Object>> items = new ArrayList<>();
        for (UserQuestionCollection c : rows) {
            InterviewKnowledge k = knowledgeMap.get(c.getKnowledgeId());
            if (knowledgeId != null && !Objects.equals(c.getKnowledgeId(), knowledgeId)) continue;
            if (StringUtils.hasText(jobPosition) && (k == null || !jobPosition.equals(k.getJobPosition()))) continue;
            if (StringUtils.hasText(q)) {
                String hay = ((k != null ? k.getQuestion() : "") + " " + Optional.ofNullable(c.getRemark()).orElse("")).toLowerCase();
                if (!hay.contains(q.toLowerCase())) continue;
            }
            items.add(InterviewMaps.collectionItem(c, k));
        }
        return Map.of("items", items);
    }

    @Transactional
    public Map<String, Object> create(Long userId, Map<String, Object> body) {
        Long knowledgeId = Long.valueOf(InterviewJsonHelper.asInt(body.get("knowledge_id"), 0));
        if (knowledgeId == null || knowledgeId <= 0) {
            throw new InterviewServiceException("missing_or_empty_field", 400, Map.of("field", "knowledge_id"));
        }
        InterviewKnowledge k = knowledgeRepository.findById(knowledgeId)
                .orElseThrow(() -> new InterviewServiceException("knowledge_not_found", 404));
        UserQuestionCollection c = collectionRepository.findByUserIdAndKnowledgeId(userId, knowledgeId)
                .orElseGet(UserQuestionCollection::new);
        c.setUserId(userId);
        c.setKnowledgeId(knowledgeId);
        if (body.containsKey("remark")) {
            c.setRemark(InterviewJsonHelper.asStr(body.get("remark")));
        }
        c = collectionRepository.save(c);
        return InterviewMaps.collectionItem(c, k);
    }

    @Transactional
    public Map<String, Object> updateRemark(Long userId, Map<String, Object> body) {
        Long id = Long.valueOf(InterviewJsonHelper.asInt(body.get("id"), 0));
        if (id == null || id <= 0) {
            throw new InterviewServiceException("missing_or_empty_field", 400, Map.of("field", "id"));
        }
        UserQuestionCollection c = collectionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new InterviewServiceException("not_found", 404));
        c.setRemark(InterviewJsonHelper.asStr(body.get("remark")));
        c = collectionRepository.save(c);
        InterviewKnowledge k = knowledgeRepository.findById(c.getKnowledgeId()).orElse(null);
        return InterviewMaps.collectionItem(c, k);
    }

    @Transactional
    public Map<String, Object> delete(Long userId, Long id) {
        if (id == null || id <= 0) {
            throw new InterviewServiceException("missing_or_empty_field", 400, Map.of("field", "id"));
        }
        UserQuestionCollection c = collectionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new InterviewServiceException("not_found", 404));
        collectionRepository.delete(c);
        return Map.of("ok", true);
    }

    public Map<String, Object> wrongBookList(Long userId) {
        List<UserQuestionStat> stats = statRepository.findByUserIdAndIsWrongBookOrderByUpdatedAtDesc(userId, 1);
        List<Map<String, Object>> items = new ArrayList<>();
        for (UserQuestionStat stat : stats) {
            InterviewKnowledge k = knowledgeRepository.findById(stat.getKnowledgeId()).orElse(null);
            UserQuestionAttempt latest = attemptRepository
                    .findFirstByUserIdAndKnowledgeIdOrderByCreatedAtDesc(userId, stat.getKnowledgeId())
                    .orElse(null);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", stat.getId());
            item.put("knowledge_id", stat.getKnowledgeId());
            item.put("title", k != null ? k.getQuestionType() : "");
            item.put("content", k != null ? k.getQuestion() : "");
            item.put("source", "岗位题库");
            item.put("job_position", k != null ? k.getJobPosition() : "");
            item.put("question_type", k != null ? k.getQuestionType() : "");
            item.put("difficulty", k != null ? k.getDifficulty() : 3);
            item.put("knowledge_point", k != null ? k.getKeywords() : "");
            item.put("assessment_focus", k != null ? k.getQuestionIntent() : "");
            item.put("user_answer", latest != null ? latest.getUserAnswer() : "");
            item.put("correct_answer", latest != null ? latest.getReferenceAnswer()
                    : (k != null ? k.getExcellentAnswer() : ""));
            item.put("wrong_count", stat.getWrongAttempts());
            item.put("last_wrong_time", InterviewMaps.iso(stat.getLastAttemptAt()));
            items.add(item);
        }
        return Map.of("items", items);
    }
}
