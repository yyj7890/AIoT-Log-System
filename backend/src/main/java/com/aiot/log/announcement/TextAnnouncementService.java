package com.aiot.log.announcement;

import com.aiot.log.config.AnnouncementProperties;
import com.aiot.log.exception.BusinessException;
import com.aiot.log.exception.ErrorCode;
import org.springframework.stereotype.Service;

@Service
public class TextAnnouncementService {
    private final AnnouncementProperties properties; private final TextToOpusSource source; private final FixedTestAnnouncementService publisher;
    public TextAnnouncementService(AnnouncementProperties properties, TextToOpusSource source, FixedTestAnnouncementService publisher) { this.properties=properties; this.source=source; this.publisher=publisher; }
    public String publish(String deviceCode, String text) {
        if (!properties.getTts().isEnabled()) return publisher.publish(deviceCode);
        AnnouncementAudio audio = source.synthesize(text).orElseThrow(() -> new BusinessException(ErrorCode.ANNOUNCEMENT_PUBLISH_FAILED));
        return publisher.publishAudio(deviceCode, audio, "tts");
    }
}
