package com.example.appbackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "interview")
public class InterviewProperties {

    private Chroma chroma = new Chroma();
    private Tencent tencent = new Tencent();
    private Avatar avatar = new Avatar();
    private Face face = new Face();
    private Asr asr = new Asr();

    @Data
    public static class Chroma {
        private String host = "localhost";
        private int port = 8001;
        private String collection = "knowledge_base";
    }

    @Data
    public static class Tencent {
        private String secretId = "";
        private String secretKey = "";
        private String appId = "";
        private String region = "ap-guangzhou";
        private String asrModel = "";
        private String voiceType = "";
        private String codec = "pcm";
        private int sampleRate = 16000;
    }

    @Data
    public static class Avatar {
        private String appId = "";
        private String apiKey = "";
        private String apiSecret = "";
        private String sceneId = "";
        private String serverUrl = "";
        private String avatarId = "";
        private String streamProtocol = "webrtc";
        private String openingGreeting = "";
    }

    @Data
    public static class Face {
        private String recordsDir = "./data/interview-face-records";
    }

    @Data
    public static class Asr {
        private String uploadDir = "./data/interview-asr";
    }
}
