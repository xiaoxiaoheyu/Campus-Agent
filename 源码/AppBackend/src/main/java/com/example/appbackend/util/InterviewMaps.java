package com.example.appbackend.util;

import com.example.appbackend.entity.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

public final class InterviewMaps {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private InterviewMaps() {
    }

    public static String iso(LocalDateTime dt) {
        return dt == null ? null : dt.format(ISO);
    }

    public static Map<String, Object> knowledgeItem(InterviewKnowledge k) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", k.getId());
        m.put("job_position", k.getJobPosition());
        m.put("question_type", k.getQuestionType());
        m.put("question", k.getQuestion());
        m.put("difficulty", k.getDifficulty() == null ? 3 : k.getDifficulty());
        m.put("status", k.getStatus() == null ? 1 : k.getStatus());
        m.put("in_chroma", k.getInChroma() == null ? 0 : k.getInChroma());
        m.put("excellent_answer", nullToEmpty(k.getExcellentAnswer()));
        m.put("answer_points", nullToEmpty(k.getAnswerPoints()));
        m.put("score_standard", nullToEmpty(k.getScoreStandard()));
        m.put("question_intent", nullToEmpty(k.getQuestionIntent()));
        m.put("keywords", nullToEmpty(k.getKeywords()));
        m.put("suitable_level", nullToEmpty(k.getSuitableLevel()));
        m.put("remark", nullToEmpty(k.getRemark()));
        m.put("created_at", iso(k.getCreatedAt()));
        m.put("updated_at", iso(k.getUpdatedAt()));
        return m;
    }

    public static Map<String, Object> conversationItem(InterviewConversation c) {
        return conversationItem(c, false, null, null, null);
    }

    public static Map<String, Object> conversationItem(InterviewConversation c,
                                                       boolean hasEvaluation,
                                                       Long evaluationId,
                                                       Integer evaluationScore,
                                                       LocalDateTime evaluationCreatedAt) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("conversation_id", c.getConversationId());
        m.put("user_id", c.getUserId());
        m.put("job_role", c.getJobRole());
        m.put("status", c.getStatus());
        m.put("has_evaluation", hasEvaluation);
        m.put("evaluation_id", evaluationId);
        m.put("evaluation_score", evaluationScore);
        m.put("evaluation_created_at", iso(evaluationCreatedAt));
        m.put("started_at", iso(c.getStartedAt()));
        m.put("ended_at", iso(c.getEndedAt()));
        m.put("created_at", iso(c.getCreatedAt()));
        m.put("updated_at", iso(c.getUpdatedAt()));
        return m;
    }

    public static Map<String, Object> configItem(InterviewConfig c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("conversation_id", c.getConversationId());
        m.put("target_position", c.getTargetPosition());
        m.put("interview_mode", c.getInterviewMode());
        m.put("interviewer_persona", c.getInterviewerPersona());
        m.put("focus_tags", c.getFocusTags());
        m.put("requirements", c.getRequirements());
        m.put("difficulty", c.getDifficulty());
        m.put("remark", c.getRemark());
        m.put("status", c.getStatus());
        m.put("created_at", iso(c.getCreatedAt()));
        m.put("updated_at", iso(c.getUpdatedAt()));
        return m;
    }

    public static Map<String, Object> collectionItem(UserQuestionCollection col, InterviewKnowledge kb) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", col.getId());
        m.put("knowledge_id", col.getKnowledgeId());
        m.put("question", kb == null ? "" : kb.getQuestion());
        m.put("job_position", kb == null ? "" : kb.getJobPosition());
        m.put("question_type", kb == null ? "" : kb.getQuestionType());
        m.put("remark", nullToEmpty(col.getRemark()));
        m.put("created_at", iso(col.getCreatedAt()));
        m.put("updated_at", iso(col.getUpdatedAt()));
        return m;
    }

    public static Map<String, Object> evaluationItem(InterviewEvaluation e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId());
        m.put("conversation_id", e.getConversationId());
        m.put("user_id", e.getUserId());
        m.put("job_role", e.getJobRole());
        m.put("score", e.getScore());
        m.put("core_conclusion", nullToEmpty(e.getCoreConclusion()));
        m.put("strengths", nullToEmpty(e.getStrengths()));
        m.put("weaknesses", nullToEmpty(e.getWeaknesses()));
        m.put("improvements", nullToEmpty(e.getImprovements()));
        m.put("created_at", iso(e.getCreatedAt()));
        m.put("updated_at", iso(e.getUpdatedAt()));
        return m;
    }

    public static Map<String, Object> messageItem(AiInterviewMessage msg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", msg.getId());
        m.put("conversation_id", msg.getConversationId());
        m.put("role", msg.getRole());
        m.put("content", msg.getContent());
        m.put("emotion_label", msg.getEmotionLabel());
        m.put("created_at", iso(msg.getCreatedAt()));
        return m;
    }

    public static Map<String, Object> profileDetail(InterviewUserProfile p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getUserId());
        m.put("nickname", p.getNickname());
        m.put("phone", p.getPhone());
        m.put("email", p.getEmail());
        m.put("gender", p.getGender());
        m.put("work_experience_years", p.getWorkExperienceYears());
        m.put("work_experience", p.getWorkExperience());
        m.put("education", p.getEducation());
        m.put("major", p.getMajor());
        m.put("graduation_year", p.getGraduationYear());
        m.put("school", p.getSchool());
        m.put("target_position", p.getTargetPosition());
        m.put("skill_tags", p.getSkillTags());
        m.put("tech_stack", p.getTechStack());
        m.put("avatar_url", p.getAvatarUrl());
        m.put("resume_url", p.getResumeUrl());
        m.put("interview_count", p.getInterviewCount());
        m.put("ai_report_count", p.getAiReportCount());
        m.put("average_score", p.getAverageScore() == null ? 0.0 : p.getAverageScore().doubleValue());
        m.put("is_manager", p.getIsManager());
        m.put("created_at", iso(p.getCreatedAt()));
        m.put("updated_at", iso(p.getUpdatedAt()));
        return m;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
