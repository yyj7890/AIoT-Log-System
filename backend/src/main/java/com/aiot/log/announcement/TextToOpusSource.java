package com.aiot.log.announcement;

import java.util.Optional;

/** A local TTS gateway returns already packetized Opus; MQTT payloads stay binary. */
public interface TextToOpusSource {
    Optional<AnnouncementAudio> synthesize(String text);
}
