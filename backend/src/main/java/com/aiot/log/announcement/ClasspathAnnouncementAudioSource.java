package com.aiot.log.announcement;

import com.aiot.log.config.AnnouncementProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class ClasspathAnnouncementAudioSource implements AnnouncementAudioSource {
    private final AnnouncementProperties properties;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    public ClasspathAnnouncementAudioSource(AnnouncementProperties properties, ResourceLoader resourceLoader,
                                            ObjectMapper objectMapper) {
        this.properties = properties;
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<AnnouncementAudio> loadFixedTestAudio() {
        try {
            Resource descriptor = resourceLoader.getResource(properties.getFixedTestResource());
            if (!descriptor.exists() || !descriptor.isReadable()) return Optional.empty();
            try (InputStream input = descriptor.getInputStream()) {
                JsonNode root = objectMapper.readTree(input);
                if (!"opus".equals(root.path("codec").asText())
                        || root.path("sampleRate").asInt() != 16000
                        || root.path("channels").asInt() != 1
                        || root.path("frameDurationMs").asInt() != 60
                        || !root.path("frames").isArray()) {
                    return Optional.empty();
                }
                List<AnnouncementAudioFrame> frames = new ArrayList<>();
                int expectedIndex = 0;
                for (JsonNode frame : root.path("frames")) {
                    if (frame.path("index").asInt(-1) != expectedIndex) return Optional.empty();
                    String resourceName = frame.path("resource").asText();
                    if (resourceName.isBlank()) return Optional.empty();
                    Resource packet = descriptor.createRelative(resourceName);
                    if (!packet.exists() || !packet.isReadable()) return Optional.empty();
                    try (InputStream packetInput = packet.getInputStream()) {
                        byte[] payload = packetInput.readAllBytes();
                        if (payload.length == 0 || payload.length > 1500) return Optional.empty();
                        frames.add(new AnnouncementAudioFrame(expectedIndex, payload, null));
                    }
                    expectedIndex++;
                }
                return frames.isEmpty() ? Optional.empty()
                        : Optional.of(new AnnouncementAudio("opus", 16000, 1, 60, List.copyOf(frames)));
            }
        } catch (Exception ignored) {
            // A missing or malformed optional test resource must never prevent normal MQTT reporting.
            return Optional.empty();
        }
    }
}
