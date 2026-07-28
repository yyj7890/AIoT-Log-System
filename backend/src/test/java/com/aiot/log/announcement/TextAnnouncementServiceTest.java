package com.aiot.log.announcement;

import com.aiot.log.config.AnnouncementProperties;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class TextAnnouncementServiceTest {
    @Test void keepsFixedAudioWhenTtsIsDisabled() {
        AnnouncementProperties properties = new AnnouncementProperties();
        TextToOpusSource source = mock(TextToOpusSource.class);
        FixedTestAnnouncementService publisher = mock(FixedTestAnnouncementService.class);
        when(publisher.publish("DEVICE-001")).thenReturn("fixed-task");
        new TextAnnouncementService(properties, source, publisher).publish("DEVICE-001", "喝水");
        verify(publisher).publish("DEVICE-001"); verifyNoInteractions(source);
    }
    @Test void publishesGatewayAudioWhenTtsIsEnabled() {
        AnnouncementProperties properties = new AnnouncementProperties(); properties.getTts().setEnabled(true);
        AnnouncementAudio audio = new AnnouncementAudio("opus", 16000, 1, 60, List.of(new AnnouncementAudioFrame(0, new byte[]{1}, null)));
        TextToOpusSource source = mock(TextToOpusSource.class);
        FixedTestAnnouncementService publisher = mock(FixedTestAnnouncementService.class);
        when(source.synthesize("喝水")).thenReturn(java.util.Optional.of(audio));
        when(publisher.publishAudio(eq("DEVICE-001"), any(), eq("tts"))).thenReturn("tts-task");
        new TextAnnouncementService(properties, source, publisher).publish("DEVICE-001", "喝水");
        verify(publisher).publishAudio("DEVICE-001", audio, "tts");
    }
}
