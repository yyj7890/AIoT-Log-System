package com.aiot.log.announcement;

import com.aiot.log.config.AnnouncementProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClasspathAnnouncementAudioSourceTest {

    @Test
    void loadsTheBundledFixedTestPackets() {
        AnnouncementProperties properties = new AnnouncementProperties();
        ClasspathAnnouncementAudioSource source = new ClasspathAnnouncementAudioSource(
                properties, new DefaultResourceLoader(), new ObjectMapper().findAndRegisterModules());

        Optional<AnnouncementAudio> audio = source.loadFixedTestAudio();

        assertTrue(audio.isPresent());
        assertEquals("opus", audio.get().codec());
        assertEquals(16000, audio.get().sampleRate());
        assertEquals(1, audio.get().channels());
        assertEquals(60, audio.get().frameDurationMs());
        assertEquals(35, audio.get().frames().size());
    }
}
