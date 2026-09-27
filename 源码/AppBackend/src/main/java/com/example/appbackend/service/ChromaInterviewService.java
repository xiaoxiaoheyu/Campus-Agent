package com.example.appbackend.service;

import com.example.appbackend.config.InterviewProperties;
import com.example.appbackend.entity.InterviewKnowledge;
import com.example.appbackend.exception.InterviewServiceException;
import com.example.appbackend.repository.InterviewKnowledgeRepository;
import com.example.appbackend.util.InterviewJsonHelper;
import com.example.appbackend.util.InterviewMaps;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.*;

@Service
public class ChromaInterviewService {

    private final InterviewKnowledgeRepository knowledgeRepository;
    private final InterviewProperties properties;
    private final ObjectMapper objectMapper;
    private final WebClient.Builder webClientBuilder;

    public ChromaInterviewService(InterviewKnowledgeRepository knowledgeRepository,
                                  InterviewProperties properties,
                                  ObjectMapper objectMapper,
                                  WebClient.Builder webClientBuilder) {
        this.knowledgeRepository = knowledgeRepository;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.webClientBuilder = webClientBuilder;
    }

    @Transactional
    public Map<String, Object> create(Long id) {
        InterviewKnowledge k = knowledgeRepository.findById(id)
                .orElseThrow(() -> new InterviewServiceException("not_found", 404));
        return upsertKnowledge(k);
    }

    @Transactional
    public Map<String, Object> upsertKnowledge(InterviewKnowledge k) {
        String docId = "kb-" + k.getId();
        Map<String, Object> metadata = buildMetadata(k);
        String document = buildDocument(k);
        boolean chromaOk = false;
        try {
            chromaOk = tryChromaUpsert(docId, document, metadata);
        } catch (Exception ignored) {
            chromaOk = false;
        }
        k.setInChroma(1);
        knowledgeRepository.save(k);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("ok", true);
        resp.put("doc_id", docId);
        resp.put("in_chroma", 1);
        resp.put("metadata", metadata);
        resp.put("chroma_synced", chromaOk);
        return resp;
    }

    public Map<String, Object> match(String q, int k, String jobPosition, String questionType, Integer status) {
        if (!StringUtils.hasText(q)) {
            throw new InterviewServiceException("missing_or_empty_field", 400, Map.of("field", "q"));
        }
        int topK = Math.min(Math.max(k, 1), 20);
        try {
            List<Map<String, Object>> chromaItems = tryChromaQuery(q, topK, jobPosition, questionType, status);
            if (chromaItems != null) {
                return Map.of("items", chromaItems);
            }
        } catch (Exception e) {
            // fallback below
        }
        List<InterviewKnowledge> rows = knowledgeRepository.keywordMatch(
                q.trim(), jobPosition, questionType, status, PageRequest.of(0, topK));
        List<Map<String, Object>> items = new ArrayList<>();
        int i = 0;
        for (InterviewKnowledge row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("doc_id", "kb-" + row.getId());
            item.put("knowledge_id", row.getId());
            item.put("distance", 0.1 * (i + 1));
            item.put("document", buildDocument(row));
            item.put("metadata", buildMetadata(row));
            item.put("knowledge", InterviewMaps.knowledgeItem(row));
            items.add(item);
            i++;
        }
        return Map.of("items", items);
    }

    private boolean tryChromaUpsert(String docId, String document, Map<String, Object> metadata) {
        String base = chromaBaseUrl();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("ids", List.of(docId));
        payload.put("documents", List.of(document));
        payload.put("metadatas", List.of(metadata));
        try {
            webClientBuilder.build()
                    .post()
                    .uri(base + "/api/v1/collections/" + properties.getChroma().getCollection() + "/upsert")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(Duration.ofSeconds(5))
                    .block();
            return true;
        } catch (Exception e) {
            // try collection create then upsert once more is optional; treat as soft failure
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> tryChromaQuery(String q, int topK, String jobPosition,
                                                     String questionType, Integer status) {
        String base = chromaBaseUrl();
        Map<String, Object> where = new LinkedHashMap<>();
        if (StringUtils.hasText(jobPosition)) where.put("job_position", jobPosition);
        if (StringUtils.hasText(questionType)) where.put("question_type", questionType);
        if (status != null) where.put("status", status);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("query_texts", List.of(q));
        payload.put("n_results", topK);
        if (!where.isEmpty()) payload.put("where", where);
        try {
            Map<?, ?> resp = webClientBuilder.build()
                    .post()
                    .uri(base + "/api/v1/collections/" + properties.getChroma().getCollection() + "/query")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            if (resp == null) return null;
            List<List<String>> ids = (List<List<String>>) resp.get("ids");
            List<List<String>> docs = (List<List<String>>) resp.get("documents");
            List<List<Double>> distances = (List<List<Double>>) resp.get("distances");
            List<List<Map<String, Object>>> metadatas = (List<List<Map<String, Object>>>) resp.get("metadatas");
            if (ids == null || ids.isEmpty()) return List.of();
            List<String> idRow = ids.get(0);
            List<Map<String, Object>> items = new ArrayList<>();
            for (int i = 0; i < idRow.size(); i++) {
                String docId = idRow.get(i);
                Long knowledgeId = resolveKnowledgeId(docId,
                        metadatas != null && !metadatas.isEmpty() && i < metadatas.get(0).size()
                                ? metadatas.get(0).get(i) : null);
                InterviewKnowledge knowledge = knowledgeId == null ? null
                        : knowledgeRepository.findById(knowledgeId).orElse(null);
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("doc_id", docId);
                item.put("knowledge_id", knowledgeId);
                item.put("distance", distances != null && !distances.isEmpty() && i < distances.get(0).size()
                        ? distances.get(0).get(i) : null);
                item.put("document", docs != null && !docs.isEmpty() && i < docs.get(0).size()
                        ? docs.get(0).get(i) : null);
                item.put("metadata", metadatas != null && !metadatas.isEmpty() && i < metadatas.get(0).size()
                        ? metadatas.get(0).get(i) : Map.of());
                item.put("knowledge", knowledge == null ? null : InterviewMaps.knowledgeItem(knowledge));
                items.add(item);
            }
            return items;
        } catch (Exception e) {
            return null;
        }
    }

    private Long resolveKnowledgeId(String docId, Map<String, Object> metadata) {
        if (metadata != null && metadata.get("id") != null) {
            Integer id = InterviewJsonHelper.asInt(metadata.get("id"), null);
            if (id != null && id > 0) return id.longValue();
        }
        if (docId != null && docId.startsWith("kb-")) {
            try {
                return Long.parseLong(docId.substring(3));
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private String chromaBaseUrl() {
        return "http://" + properties.getChroma().getHost() + ":" + properties.getChroma().getPort();
    }

    private Map<String, Object> buildMetadata(InterviewKnowledge k) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("model", "KnowledgeBase");
        m.put("id", k.getId());
        m.put("job_position", k.getJobPosition());
        m.put("question_type", k.getQuestionType());
        m.put("difficulty", k.getDifficulty());
        m.put("status", k.getStatus());
        m.put("keywords", k.getKeywords());
        m.put("created_at", InterviewMaps.iso(k.getCreatedAt()));
        m.put("updated_at", InterviewMaps.iso(k.getUpdatedAt()));
        return m;
    }

    private String buildDocument(InterviewKnowledge k) {
        return String.join("\n",
                "岗位：" + nullSafe(k.getJobPosition()),
                "题型：" + nullSafe(k.getQuestionType()),
                "题目：" + nullSafe(k.getQuestion()),
                "关键词：" + nullSafe(k.getKeywords()),
                "考察意图：" + nullSafe(k.getQuestionIntent()),
                "答题要点：" + nullSafe(k.getAnswerPoints()),
                "评分标准：" + nullSafe(k.getScoreStandard()),
                "优秀答案：" + nullSafe(k.getExcellentAnswer()),
                "适用层级：" + nullSafe(k.getSuitableLevel()),
                "备注：" + nullSafe(k.getRemark())
        );
    }

    private static String nullSafe(String v) {
        return v == null ? "" : v;
    }
}
