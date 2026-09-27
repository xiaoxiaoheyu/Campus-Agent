package com.example.appbackend.controller;

import com.example.appbackend.service.InterviewAsrTtsAvatarService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
public class InterviewMediaController {

    private final InterviewAsrTtsAvatarService mediaService;

    public InterviewMediaController(InterviewAsrTtsAvatarService mediaService) {
        this.mediaService = mediaService;
    }

    @PostMapping("/api/asr/upload")
    public ResponseEntity<?> asrUpload(@RequestParam("audio") MultipartFile audio,
                                       @RequestParam(value = "voice_format", required = false) String voiceFormat) {
        return ResponseEntity.ok(mediaService.asrUpload(audio, voiceFormat));
    }

    @PostMapping("/api/asr/transcribe")
    public ResponseEntity<?> asrTranscribe(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(mediaService.asrTranscribe(body));
    }

    @PostMapping("/api/tts/synthesize")
    public ResponseEntity<?> tts(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(mediaService.ttsSynthesize(body));
    }

    @GetMapping("/api/avatar/runtime-config")
    public ResponseEntity<?> avatarRuntimeConfig() {
        return ResponseEntity.ok(mediaService.avatarRuntimeConfig());
    }

    @PostMapping("/api/avatar/audio/frames")
    public ResponseEntity<?> avatarFrames(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(mediaService.avatarAudioFrames(body));
    }

    @PostMapping("/api/avatar/session/start")
    public ResponseEntity<?> avatarStart(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(mediaService.avatarSessionStart(body));
    }

    @PostMapping("/api/avatar/session/speak")
    public ResponseEntity<?> avatarSpeak(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(mediaService.avatarSessionSpeak(body));
    }

    @PostMapping("/api/avatar/session/interrupt")
    public ResponseEntity<?> avatarInterrupt(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(mediaService.avatarSessionSimple(body));
    }

    @PostMapping("/api/avatar/session/stop")
    public ResponseEntity<?> avatarStop(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(mediaService.avatarSessionSimple(body));
    }
}
