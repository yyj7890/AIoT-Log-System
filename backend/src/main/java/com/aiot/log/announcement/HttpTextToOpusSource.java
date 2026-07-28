package com.aiot.log.announcement;

import com.aiot.log.config.AnnouncementProperties;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Component
public class HttpTextToOpusSource implements TextToOpusSource {
    private final AnnouncementProperties properties;
    public HttpTextToOpusSource(AnnouncementProperties properties) { this.properties = properties; }
    @Override public Optional<AnnouncementAudio> synthesize(String text) {
        var tts = properties.getTts();
        if (!tts.isEnabled() || tts.getBaseUrl().isBlank() || text == null || text.isBlank()) return Optional.empty();
        try {
            Duration timeout = Duration.ofMillis(Math.max(1, tts.getTimeoutMs()));
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(timeout);
            requestFactory.setReadTimeout(timeout);
            Response response = RestClient.builder().baseUrl(tts.getBaseUrl()).requestFactory(requestFactory).build()
                    .post().uri("/v1/announcements/opus")
                    .contentType(MediaType.APPLICATION_JSON).body(new Request(text.trim(), 16000, 1, 60))
                    .retrieve().body(Response.class);
            if (response == null || response.frames() == null) return Optional.empty();
            return Optional.of(new AnnouncementAudio(response.codec(), response.sampleRate(), response.channels(),
                    response.frameDurationMs(), response.frames().stream().map(f -> new AnnouncementAudioFrame(f.index(),
                    Base64.getDecoder().decode(f.payloadBase64()), null)).toList()));
        } catch (RuntimeException ignored) { return Optional.empty(); }
    }
    record Request(String text, int sampleRate, int channels, int frameDurationMs) {}
    record Response(String codec, int sampleRate, int channels, int frameDurationMs, List<Frame> frames) {}
    record Frame(int index, String payloadBase64) {}
}
