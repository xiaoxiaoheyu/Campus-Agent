package com.example.appbackend.service;

import com.example.appbackend.entity.InterviewJobTechStack;
import com.example.appbackend.entity.InterviewUserProfile;
import com.example.appbackend.entity.User;
import com.example.appbackend.exception.InterviewServiceException;
import com.example.appbackend.repository.InterviewJobTechStackRepository;
import com.example.appbackend.repository.InterviewUserProfileRepository;
import com.example.appbackend.repository.UserRepository;
import com.example.appbackend.util.InterviewJsonHelper;
import com.example.appbackend.util.InterviewMaps;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Service
public class InterviewUserProfileService {

    private final InterviewUserProfileRepository profileRepository;
    private final InterviewJobTechStackRepository jobTechStackRepository;
    private final UserRepository userRepository;
    private final COSClient cosClient;

    @Value("${tencent.cos.bucket:}")
    private String bucket;

    @Value("${tencent.cos.domain:}")
    private String domain;

    @Value("${tencent.cos.upload-prefix:smart-campus/media}")
    private String uploadPrefix;

    @Value("${file.upload-dir:uploads}")
    private String localUploadDir;

    @Value("${file.base-url:http://localhost:8080}")
    private String fileBaseUrl;

    public InterviewUserProfileService(InterviewUserProfileRepository profileRepository,
                                       InterviewJobTechStackRepository jobTechStackRepository,
                                       UserRepository userRepository,
                                       COSClient cosClient) {
        this.profileRepository = profileRepository;
        this.jobTechStackRepository = jobTechStackRepository;
        this.userRepository = userRepository;
        this.cosClient = cosClient;
    }

    @Transactional
    public InterviewUserProfile getOrCreate(Long userId) {
        return profileRepository.findByUserId(userId).orElseGet(() -> {
            InterviewUserProfile p = new InterviewUserProfile();
            p.setUserId(userId);
            userRepository.findById(userId).ifPresent(u -> seedFromUser(p, u));
            return profileRepository.save(p);
        });
    }

    public Map<String, Object> detail(Long queryId, Long jwtUserId) {
        Long userId = queryId != null && queryId > 0 ? queryId : jwtUserId;
        if (userId == null || userId <= 0) {
            throw new InterviewServiceException("invalid_or_expired_session", 401);
        }
        return InterviewMaps.profileDetail(getOrCreate(userId));
    }

    @Transactional
    public Map<String, Object> update(Map<String, Object> body) {
        Long id = Long.valueOf(InterviewJsonHelper.asInt(body.get("id"), 0));
        if (id == null || id <= 0) {
            throw new InterviewServiceException("missing_or_empty_field", 400, Map.of("field", "id"));
        }
        for (String forbidden : List.of("interview_count", "ai_report_count", "average_score")) {
            if (body.containsKey(forbidden)) {
                throw new InterviewServiceException("forbidden_field", 400, Map.of("field", forbidden));
            }
        }
        InterviewUserProfile p = getOrCreate(id);
        if (body.containsKey("nickname")) p.setNickname(InterviewJsonHelper.asStr(body.get("nickname")));
        if (body.containsKey("phone")) p.setPhone(InterviewJsonHelper.asStr(body.get("phone")));
        if (body.containsKey("email")) p.setEmail(InterviewJsonHelper.asStr(body.get("email")));
        if (body.containsKey("gender")) p.setGender(InterviewJsonHelper.asInt(body.get("gender"), p.getGender()));
        if (body.containsKey("work_experience_years")) p.setWorkExperienceYears(InterviewJsonHelper.asInt(body.get("work_experience_years"), p.getWorkExperienceYears()));
        if (body.containsKey("work_experience")) p.setWorkExperience(InterviewJsonHelper.asStr(body.get("work_experience")));
        if (body.containsKey("education")) p.setEducation(InterviewJsonHelper.asStr(body.get("education")));
        if (body.containsKey("major")) p.setMajor(InterviewJsonHelper.asStr(body.get("major")));
        if (body.containsKey("graduation_year")) p.setGraduationYear(InterviewJsonHelper.asInt(body.get("graduation_year"), p.getGraduationYear()));
        if (body.containsKey("school")) p.setSchool(InterviewJsonHelper.asStr(body.get("school")));
        if (body.containsKey("target_position")) p.setTargetPosition(InterviewJsonHelper.asStr(body.get("target_position")));
        if (body.containsKey("skill_tags")) p.setSkillTags(InterviewJsonHelper.asStr(body.get("skill_tags")));
        if (body.containsKey("tech_stack")) p.setTechStack(InterviewJsonHelper.asStr(body.get("tech_stack")));
        if (body.containsKey("avatar_url")) p.setAvatarUrl(InterviewJsonHelper.asStr(body.get("avatar_url")));
        if (body.containsKey("resume_url")) p.setResumeUrl(InterviewJsonHelper.asStr(body.get("resume_url")));
        p = profileRepository.save(p);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("id", p.getUserId());
        resp.put("nickname", p.getNickname());
        resp.put("phone", p.getPhone());
        resp.put("email", p.getEmail());
        resp.put("avatar_url", p.getAvatarUrl());
        resp.put("updated_at", InterviewMaps.iso(p.getUpdatedAt()));
        return resp;
    }

    @Transactional
    public Map<String, Object> updateMetrics(Map<String, Object> body) {
        Long id = Long.valueOf(InterviewJsonHelper.asInt(body.get("id"), 0));
        if (id == null || id <= 0) {
            throw new InterviewServiceException("missing_or_empty_field", 400, Map.of("field", "id"));
        }
        boolean any = body.containsKey("interview_count") || body.containsKey("ai_report_count") || body.containsKey("average_score");
        if (!any) {
            throw new InterviewServiceException("missing_or_empty_field", 400, Map.of("field", "metrics"));
        }
        InterviewUserProfile p = getOrCreate(id);
        if (body.containsKey("interview_count")) p.setInterviewCount(InterviewJsonHelper.asInt(body.get("interview_count"), p.getInterviewCount()));
        if (body.containsKey("ai_report_count")) p.setAiReportCount(InterviewJsonHelper.asInt(body.get("ai_report_count"), p.getAiReportCount()));
        if (body.containsKey("average_score")) {
            try {
                p.setAverageScore(new BigDecimal(String.valueOf(body.get("average_score"))));
            } catch (Exception e) {
                throw new InterviewServiceException("invalid_average_score", 400);
            }
        }
        p = profileRepository.save(p);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("id", p.getUserId());
        resp.put("interview_count", p.getInterviewCount());
        resp.put("ai_report_count", p.getAiReportCount());
        resp.put("average_score", p.getAverageScore());
        resp.put("updated_at", InterviewMaps.iso(p.getUpdatedAt()));
        return resp;
    }

    public Map<String, Object> jobPositions() {
        List<Map<String, Object>> items = jobTechStackRepository.findByStatusOrderByIdAsc(1).stream().map(j -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", j.getId());
            m.put("job_position", j.getJobPosition());
            m.put("tech_stack", j.getTechStack());
            return m;
        }).toList();
        return Map.of("items", items);
    }

    @Transactional
    public Map<String, Object> uploadAvatar(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InterviewServiceException("missing_or_empty_field", 400, Map.of("field", "file"));
        }
        if (file.getSize() > 5L * 1024 * 1024) {
            throw new InterviewServiceException("file_too_large", 400);
        }
        String original = Optional.ofNullable(file.getOriginalFilename()).orElse("avatar.png");
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.')).toLowerCase() : ".png";
        if (!Set.of(".jpg", ".jpeg", ".png", ".webp", ".gif").contains(ext)) {
            throw new InterviewServiceException("invalid_file_type", 400);
        }
        String key = uploadPrefix + "/interview-avatars/" + UUID.randomUUID() + ext;
        String url;
        String etag = "";
        String usedBucket = bucket;
        try {
            if (StringUtils.hasText(bucket) && StringUtils.hasText(domain)) {
                ObjectMetadata meta = new ObjectMetadata();
                meta.setContentLength(file.getSize());
                meta.setContentType(file.getContentType());
                try (InputStream in = file.getInputStream()) {
                    PutObjectRequest put = new PutObjectRequest(bucket, key, in, meta);
                    var result = cosClient.putObject(put);
                    etag = result.getETag();
                }
                url = domain.endsWith("/") ? domain + key : domain + "/" + key;
            } else {
                Path dir = Paths.get(localUploadDir, "interview-avatars").toAbsolutePath().normalize();
                Files.createDirectories(dir);
                String filename = UUID.randomUUID() + ext;
                Path target = dir.resolve(filename);
                try (InputStream in = file.getInputStream()) {
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                }
                key = "interview-avatars/" + filename;
                usedBucket = "local";
                String base = fileBaseUrl.endsWith("/") ? fileBaseUrl.substring(0, fileBaseUrl.length() - 1) : fileBaseUrl;
                url = base + "/uploads/" + key;
            }
        } catch (InterviewServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new InterviewServiceException("cos_upload_failed", 500);
        }
        InterviewUserProfile p = getOrCreate(userId);
        p.setAvatarUrl(url);
        profileRepository.save(p);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("url", url);
        resp.put("key", key);
        resp.put("bucket", usedBucket);
        resp.put("etag", etag);
        return resp;
    }

    private void seedFromUser(InterviewUserProfile p, User u) {
        p.setNickname(Optional.ofNullable(u.getRealName()).orElse(u.getUsername()));
        p.setPhone(u.getPhone());
        p.setEmail(u.getEmail());
        p.setMajor(u.getMajor());
        p.setSchool(u.getCollege());
        p.setAvatarUrl(u.getAvatar());
        if (u.getRole() != null && u.getRole().getName() != null) {
            String roleName = u.getRole().getName().toLowerCase(Locale.ROOT);
            if (roleName.contains("admin") || roleName.contains("manager") || roleName.contains("管理")) {
                p.setIsManager(1);
            }
        }
    }
}
