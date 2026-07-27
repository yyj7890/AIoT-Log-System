package com.aiot.log.announcement;

import java.util.Optional;

public interface AnnouncementAudioSource {
    Optional<AnnouncementAudio> loadFixedTestAudio();
}
