package com.example.appbackend.service;

import com.example.appbackend.config.InterviewProperties;
import com.example.appbackend.util.InterviewJsonHelper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class InterviewFaceRecordService {

    private final InterviewProperties properties;
    private final ObjectMapper objectMapper;

    public InterviewFaceRecordService(InterviewProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> start(Map<String, Object> body) {
        String conversationId = sanitize(InterviewJsonHelper.asStr(body.get("conversation_id")));
        if (!StringUtils.hasText(conversationId)) {
            return fail("conversation_id required");
        }
        try {
            Path file = recordPath(conversationId);
            Files.createDirectories(file.getParent());
            Map<String, Object> doc = new LinkedHashMap<>();
            doc.put("conversation_id", conversationId);
            doc.put("start_time", LocalDateTime.now().toString());
            doc.put("end_time", null);
            doc.put("detect_fps", InterviewJsonHelper.asInt(body.get("detect_fps"), 5));
            doc.put("total_records", 0);
            doc.put("records", new ArrayList<>());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), doc);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("conversation_id", conversationId);
            data.put("file_path", file.toString());
            data.put("auto_capture", body.getOrDefault("auto_capture", true));
            data.put("captured_records", 0);
            data.put("total_records", 0);
            data.put("capture_error", null);
            Map<String, Object> resp = ok();
            resp.putAll(data);
            resp.put("data", data);
            return resp;
        } catch (Exception e) {
            return fail(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> append(Map<String, Object> body) {
        String conversationId = sanitize(first(body, "conversation_id", "session_id"));
        if (!StringUtils.hasText(conversationId)) {
            return fail("conversation_id required");
        }
        try {
            Path file = recordPath(conversationId);
            Map<String, Object> doc;
            if (Files.exists(file)) {
                doc = objectMapper.readValue(file.toFile(), new TypeReference<>() {});
            } else {
                doc = new LinkedHashMap<>();
                doc.put("conversation_id", conversationId);
                doc.put("start_time", LocalDateTime.now().toString());
                doc.put("records", new ArrayList<>());
            }
            List<Map<String, Object>> records = (List<Map<String, Object>>) doc.computeIfAbsent("records", k -> new ArrayList<>());
            Object recordsObj = body.get("records");
            if (recordsObj instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> m) {
                        records.add(new LinkedHashMap<>((Map<String, Object>) m));
                    }
                }
            }
            if (body.get("frame_image") != null) {
                Map<String, Object> frame = new LinkedHashMap<>();
                frame.put("capture_time", body.getOrDefault("capture_time", LocalDateTime.now().toString()));
                frame.put("face_detected", true);
                frame.put("frame_image", body.get("frame_image"));
                frame.put("blendshapes", Map.of());
                records.add(frame);
            }
            doc.put("total_records", records.size());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), doc);
            Map<String, Object> data = Map.of("total_records", records.size());
            Map<String, Object> resp = ok();
            resp.put("total_records", records.size());
            resp.put("data", data);
            return resp;
        } catch (Exception e) {
            return fail(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> end(Map<String, Object> body) {
        String conversationId = sanitize(InterviewJsonHelper.asStr(body.get("conversation_id")));
        if (!StringUtils.hasText(conversationId)) {
            return fail("conversation_id required");
        }
        try {
            Path file = recordPath(conversationId);
            Map<String, Object> doc = Files.exists(file)
                    ? objectMapper.readValue(file.toFile(), new TypeReference<>() {})
                    : new LinkedHashMap<>();
            doc.put("end_time", LocalDateTime.now().toString());
            List<?> records = (List<?>) doc.getOrDefault("records", List.of());
            doc.put("total_records", records.size());
            Files.createDirectories(file.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), doc);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("file_path", file.toString());
            data.put("total_records", records.size());
            Map<String, Object> resp = ok();
            resp.putAll(data);
            resp.put("data", data);
            return resp;
        } catch (Exception e) {
            return fail(e.getMessage());
        }
    }

    public Map<String, Object> list() {
        try {
            Path dir = Paths.get(properties.getFace().getRecordsDir()).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            List<Map<String, Object>> items = new ArrayList<>();
            DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            try (var stream = Files.list(dir)) {
                stream.filter(p -> p.getFileName().toString().endsWith(".json")).forEach(p -> {
                    try {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("filename", p.getFileName().toString());
                        item.put("size", Files.size(p));
                        item.put("created_at", Files.getLastModifiedTime(p).toInstant().toString());
                        item.put("modified_at", Files.getLastModifiedTime(p).toInstant().toString());
                        items.add(item);
                    } catch (Exception ignored) {
                    }
                });
            }
            Map<String, Object> resp = ok();
            resp.put("data", items);
            return resp;
        } catch (Exception e) {
            return fail(e.getMessage());
        }
    }

    public Map<String, Object> detail(String conversationId) {
        try {
            Path file = recordPath(sanitize(conversationId));
            if (!Files.exists(file)) {
                return fail("not found");
            }
            Map<String, Object> doc = objectMapper.readValue(file.toFile(), new TypeReference<>() {});
            Map<String, Object> resp = ok();
            resp.put("data", doc);
            return resp;
        } catch (Exception e) {
            return fail(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> emotionSummary(String conversationId, int smoothWindow, double neutralMargin, double minConfidence) {
        try {
            Path file = recordPath(sanitize(conversationId));
            if (!Files.exists(file)) {
                return fail("not found");
            }
            Map<String, Object> doc = objectMapper.readValue(file.toFile(), new TypeReference<>() {});
            List<Map<String, Object>> records = (List<Map<String, Object>>) doc.getOrDefault("records", List.of());

            List<Map<String, Object>> perSecond = new ArrayList<>();
            double tensionSum = 0;
            double tensionMax = 0;
            double positivitySum = 0;
            double positivityMax = 0;
            int faceFrames = 0;
            Map<String, Integer> frameDist = new LinkedHashMap<>();

            int second = 0;
            int idx = 0;
            int bucketSize = Math.max(1, smoothWindow);
            while (idx < records.size()) {
                int end = Math.min(idx + bucketSize, records.size());
                Map<String, Integer> labelDist = new LinkedHashMap<>();
                double tSum = 0, tMax = 0, pSum = 0, pMax = 0;
                int faces = 0;
                for (int i = idx; i < end; i++) {
                    Map<String, Object> rec = records.get(i);
                    boolean face = Boolean.TRUE.equals(rec.get("face_detected"));
                    if (face) faces++;
                    Map<String, Object> blend = rec.get("blendshapes") instanceof Map<?, ?> m
                            ? (Map<String, Object>) m : Map.of();
                    double tension = tensionScore(blend);
                    double positivity = positivityScore(blend);
                    String label = classify(tension, positivity, neutralMargin, minConfidence, face);
                    labelDist.merge(label, 1, Integer::sum);
                    frameDist.merge(label, 1, Integer::sum);
                    tSum += tension;
                    tMax = Math.max(tMax, tension);
                    pSum += positivity;
                    pMax = Math.max(pMax, positivity);
                }
                int frames = end - idx;
                String main = labelDist.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("no_face");
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("second", second);
                row.put("frames", frames);
                row.put("face_frames", faces);
                row.put("face_detect_ratio", frames == 0 ? 0.0 : (double) faces / frames);
                row.put("main_label", main);
                row.put("dominant_expression", main);
                row.put("emotion", main);
                row.put("avg_tension_score", frames == 0 ? 0.0 : tSum / frames);
                row.put("max_tension_score", tMax);
                row.put("avg_positivity_score", frames == 0 ? 0.0 : pSum / frames);
                row.put("max_positivity_score", pMax);
                row.put("label_distribution", labelDist);
                perSecond.add(row);
                tensionSum += tSum;
                tensionMax = Math.max(tensionMax, tMax);
                positivitySum += pSum;
                positivityMax = Math.max(positivityMax, pMax);
                faceFrames += faces;
                idx = end;
                second++;
            }

            int total = records.size();
            String overallState = frameDist.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("no_face");
            Map<String, Object> overall = new LinkedHashMap<>();
            overall.put("total_seconds", perSecond.size());
            overall.put("overall_state", overallState);
            overall.put("avg_tension_score", total == 0 ? 0.0 : tensionSum / total);
            overall.put("max_tension_score", tensionMax);
            overall.put("avg_positivity_score", total == 0 ? 0.0 : positivitySum / total);
            overall.put("max_positivity_score", positivityMax);
            overall.put("face_detect_ratio", total == 0 ? 0.0 : (double) faceFrames / total);
            overall.put("frame_distribution", frameDist);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("overall", overall);
            data.put("per_second", perSecond);
            Map<String, Object> resp = ok();
            resp.put("conversation_id", conversationId);
            resp.put("data", data);
            return resp;
        } catch (Exception e) {
            return fail(e.getMessage());
        }
    }

    private double tensionScore(Map<String, Object> blend) {
        double browUp = avg(blend, "browInnerUp", "browOuterUpLeft", "browOuterUpRight");
        double browDown = avg(blend, "browDownLeft", "browDownRight");
        double press = avg(blend, "mouthPressLeft", "mouthPressRight");
        double blink = avg(blend, "eyeBlinkLeft", "eyeBlinkRight");
        double smile = avg(blend, "mouthSmileLeft", "mouthSmileRight");
        return clip(0.26 * browUp + 0.22 * browDown + 0.22 * press + 0.12 * blink - 0.2 * smile);
    }

    private double positivityScore(Map<String, Object> blend) {
        double smile = avg(blend, "mouthSmileLeft", "mouthSmileRight");
        double frown = avg(blend, "mouthFrownLeft", "mouthFrownRight");
        return clip(smile - 0.5 * frown);
    }

    private String classify(double tension, double positivity, double neutralMargin, double minConfidence, boolean face) {
        if (!face) return "no_face";
        if (positivity > minConfidence && positivity - tension > neutralMargin) return "positive";
        if (tension > minConfidence && tension - positivity > neutralMargin) return "tense";
        return "stable";
    }

    private double avg(Map<String, Object> blend, String... keys) {
        double sum = 0;
        int n = 0;
        for (String k : keys) {
            Object v = blend.get(k);
            if (v == null) continue;
            try {
                sum += Double.parseDouble(String.valueOf(v));
                n++;
            } catch (Exception ignored) {
            }
        }
        return n == 0 ? 0 : sum / n;
    }

    private double clip(double v) {
        if (v < 0) return 0;
        if (v > 1) return 1;
        return v;
    }

    private Path recordPath(String conversationId) {
        return Paths.get(properties.getFace().getRecordsDir()).toAbsolutePath().normalize()
                .resolve(conversationId + ".json");
    }

    private String sanitize(String value) {
        if (value == null) return null;
        return value.trim().replaceAll("[^0-9A-Za-z_-]", "_");
    }

    private String first(Map<String, Object> body, String... keys) {
        for (String k : keys) {
            String v = InterviewJsonHelper.asStr(body.get(k));
            if (StringUtils.hasText(v)) return v;
        }
        return null;
    }

    private Map<String, Object> ok() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", 0);
        m.put("msg", "success");
        return m;
    }

    private Map<String, Object> fail(String msg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", 1);
        m.put("msg", msg == null ? "error" : msg);
        return m;
    }
}
