package com.example.appbackend.service;

import com.example.appbackend.config.InterviewProperties;
import com.example.appbackend.exception.InterviewServiceException;
import com.example.appbackend.util.InterviewJsonHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Service
public class InterviewAsrTtsAvatarService {

    private final InterviewProperties properties;

    public InterviewAsrTtsAvatarService(InterviewProperties properties) {
        this.properties = properties;
    }

    public Map<String, Object> asrUpload(MultipartFile audio, String voiceFormat) {
        if (audio == null || audio.isEmpty()) {
            throw new InterviewServiceException("missing_or_empty_field", 400, Map.of("field", "audio"));
        }
        if (audio.getSize() > 10L * 1024 * 1024) {
            throw new InterviewServiceException("file_too_large", 400);
        }
        try {
            Path dir = Paths.get(properties.getAsr().getUploadDir()).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            String original = Optional.ofNullable(audio.getOriginalFilename()).orElse("audio.wav");
            String ext = original.contains(".") ? original.substring(original.lastIndexOf('.')) : ".wav";
            String filename = UUID.randomUUID() + ext;
            Path target = dir.resolve(filename);
            Files.copy(audio.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            Map<String, Object> saved = new LinkedHashMap<>();
            saved.put("filename", filename);
            saved.put("path", target.toString());
            saved.put("size", Files.size(target));
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("saved", true);
            resp.put("message", "audio_saved");
            resp.put("voice_format", StringUtils.hasText(voiceFormat) ? voiceFormat : ext.replace(".", ""));
            resp.put("saved_file", saved);
            return resp;
        } catch (Exception e) {
            throw new InterviewServiceException("internal_error", 500);
        }
    }

    public Map<String, Object> asrTranscribe(Map<String, Object> body) {
        String filename = InterviewJsonHelper.asStr(body.get("filename"));
        if (!StringUtils.hasText(filename)) {
            throw new InterviewServiceException("missing_or_empty_field", 400, Map.of("field", "filename"));
        }
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("text", "");
        resp.put("request_id", "");
        resp.put("filename", filename);
        if (!StringUtils.hasText(properties.getTencent().getSecretId())
                || !StringUtils.hasText(properties.getTencent().getSecretKey())) {
            resp.put("message", "tencent_asr_not_configured");
        }
        return resp;
    }

    public Map<String, Object> ttsSynthesize(Map<String, Object> body) {
        String text = InterviewJsonHelper.asStr(body.get("text"));
        if (!StringUtils.hasText(text)) {
            throw new InterviewServiceException("missing_or_empty_field", 400, Map.of("field", "text"));
        }
        String codec = Optional.ofNullable(InterviewJsonHelper.asStr(body.get("codec")))
                .filter(StringUtils::hasText)
                .orElse(properties.getTencent().getCodec());
        int sampleRate = InterviewJsonHelper.asInt(body.get("sample_rate"), properties.getTencent().getSampleRate());
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("text", text);
        resp.put("filename", "");
        resp.put("path", "");
        resp.put("request_id", "");
        resp.put("session_id", "");
        resp.put("codec", codec);
        resp.put("sample_rate", sampleRate);
        resp.put("audio_base64", "");
        if (!StringUtils.hasText(properties.getTencent().getSecretId())) {
            resp.put("message", "tencent_tts_not_configured");
        }
        return resp;
    }

    public Map<String, Object> avatarRuntimeConfig() {
        InterviewProperties.Avatar a = properties.getAvatar();
        Map<String, Object> stream = new LinkedHashMap<>();
        stream.put("protocol", Optional.ofNullable(a.getStreamProtocol()).orElse("webrtc"));
        stream.put("fallback_protocol", "flv");
        stream.put("allow_fallback", true);

        Map<String, Object> avatar = Map.of("avatar_id", Optional.ofNullable(a.getAvatarId()).orElse(""));
        Map<String, Object> audio = new LinkedHashMap<>();
        audio.put("sample_rate", 16000);
        audio.put("bit_depth", 16);
        audio.put("channels", 1);
        audio.put("default_frame_ms", 40);

        Map<String, Object> features = new LinkedHashMap<>();
        features.put("audio_driver", true);
        features.put("text_driver", false);
        features.put("text_interact", false);

        Map<String, Object> opening = new LinkedHashMap<>();
        opening.put("text", Optional.ofNullable(a.getOpeningGreeting()).orElse(""));
        opening.put("auto_speak", true);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("app_id", Optional.ofNullable(a.getAppId()).orElse(""));
        resp.put("scene_id", Optional.ofNullable(a.getSceneId()).orElse(""));
        resp.put("server_url", Optional.ofNullable(a.getServerUrl()).orElse(""));
        resp.put("stream", stream);
        resp.put("avatar", avatar);
        resp.put("audio", audio);
        resp.put("features", features);
        resp.put("opening", opening);
        return resp;
    }

    public Map<String, Object> avatarAudioFrames(Map<String, Object> body) {
        String ttsFilename = InterviewJsonHelper.asStr(body.get("tts_filename"));
        if (!StringUtils.hasText(ttsFilename)) {
            throw new InterviewServiceException("missing_or_empty_field", 400, Map.of("field", "tts_filename"));
        }
        int frameMs = InterviewJsonHelper.asInt(body.get("frame_ms"), 40);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("tts_filename", ttsFilename);
        resp.put("sample_rate", 16000);
        resp.put("bit_depth", 16);
        resp.put("channels", 1);
        resp.put("frame_ms", frameMs);
        resp.put("frames", List.of());
        resp.put("message", "avatar_audio_stub");
        return resp;
    }

    public Map<String, Object> avatarSessionStart(Map<String, Object> body) {
        Map<String, Object> stream = new LinkedHashMap<>();
        stream.put("protocol", Optional.ofNullable(InterviewJsonHelper.asStr(body.get("protocol")))
                .filter(StringUtils::hasText).orElse(properties.getAvatar().getStreamProtocol()));
        stream.put("stream_url", "");
        stream.put("stream_extend", Map.of());
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("avatar_session_id", UUID.randomUUID().toString());
        resp.put("conversation_id", body.get("conversation_id"));
        resp.put("stream", stream);
        if (!StringUtils.hasText(properties.getAvatar().getAppId())) {
            resp.put("message", "avatar_not_configured");
        }
        return resp;
    }

    public Map<String, Object> avatarSessionSpeak(Map<String, Object> body) {
        String sessionId = InterviewJsonHelper.asStr(body.get("avatar_session_id"));
        String ttsFilename = InterviewJsonHelper.asStr(body.get("tts_filename"));
        if (!StringUtils.hasText(sessionId) || !StringUtils.hasText(ttsFilename)) {
            throw new InterviewServiceException("missing_or_empty_field", 400);
        }
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("accepted", true);
        resp.put("avatar_session_id", sessionId);
        resp.put("tts_filename", ttsFilename);
        resp.put("frame_count", 0);
        resp.put("duration_ms", 0);
        resp.put("frame_ms", InterviewJsonHelper.asInt(body.get("frame_ms"), 40));
        resp.put("action_count", body.get("actions") instanceof List<?> list ? list.size() : 0);
        return resp;
    }

    public Map<String, Object> avatarSessionSimple(Map<String, Object> body) {
        String sessionId = InterviewJsonHelper.asStr(body.get("avatar_session_id"));
        if (!StringUtils.hasText(sessionId)) {
            throw new InterviewServiceException("missing_or_empty_field", 400, Map.of("field", "avatar_session_id"));
        }
        return Map.of("accepted", true, "avatar_session_id", sessionId);
    }
}
