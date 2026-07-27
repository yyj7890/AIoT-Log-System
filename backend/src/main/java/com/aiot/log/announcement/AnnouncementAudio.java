package com.aiot.log.announcement;

import java.util.List;

public record AnnouncementAudio(String codec, int sampleRate, int channels, int frameDurationMs,
                                List<AnnouncementAudioFrame> frames) {
}
