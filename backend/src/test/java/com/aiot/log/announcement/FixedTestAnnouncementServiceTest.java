package com.aiot.log.announcement;

import com.aiot.log.config.AnnouncementProperties;
import com.aiot.log.entity.AnnouncementDelivery;
import com.aiot.log.entity.Device;
import com.aiot.log.mapper.AnnouncementDeliveryMapper;
import com.aiot.log.mapper.DeviceMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.time.Duration;
import java.util.Optional;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FixedTestAnnouncementServiceTest {

    @Test
    void publishesManifestThenRawFramesWithExpectedTopicsAndCrc() throws Exception {
        AnnouncementProperties properties = new AnnouncementProperties();
        properties.setTestEnabled(true);
        AnnouncementAudioSource source = () -> Optional.of(new AnnouncementAudio("opus", 16000, 1, 60,
                List.of(new AnnouncementAudioFrame(0, new byte[]{1, 2, 3}, null),
                        new AnnouncementAudioFrame(1, new byte[]{4, 5}, null))));
        AnnouncementMqttGateway gateway = mock(AnnouncementMqttGateway.class);
        DeviceMapper deviceMapper = mock(DeviceMapper.class);
        AnnouncementDeliveryMapper deliveryMapper = mock(AnnouncementDeliveryMapper.class);
        Device device = new Device();
        device.setId(7L);
        device.setDeviceCode("DEVICE-001");
        when(deviceMapper.selectOne(any())).thenReturn(device);

        FixedTestAnnouncementService service = new FixedTestAnnouncementService(properties, source, gateway,
                deviceMapper, deliveryMapper, new ObjectMapper().findAndRegisterModules());
        String taskId = service.publish("DEVICE-001");

        ArgumentCaptor<String> topics = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<byte[]> payloads = ArgumentCaptor.forClass(byte[].class);
        verify(gateway, org.mockito.Mockito.times(3)).publish(topics.capture(), payloads.capture());
        assertEquals("aiot/device/DEVICE-001/announcement/command", topics.getAllValues().get(0));
        assertEquals("aiot/device/DEVICE-001/announcement/audio/" + taskId + "/0", topics.getAllValues().get(1));
        assertEquals("aiot/device/DEVICE-001/announcement/audio/" + taskId + "/1", topics.getAllValues().get(2));
        assertEquals(new String(new byte[]{1, 2, 3}), new String(payloads.getAllValues().get(1)));

        JsonNode manifest = new ObjectMapper().findAndRegisterModules().readTree(payloads.getAllValues().get(0));
        assertEquals("aiot-announcement-v1", manifest.path("protocol").asText());
        assertEquals(taskId, manifest.path("taskId").asText());
        assertEquals(16000, manifest.path("audio").path("sampleRate").asInt());
        assertEquals(1, manifest.path("audio").path("channels").asInt());
        assertEquals(60, manifest.path("audio").path("frameDurationMs").asInt());
        assertEquals(2, manifest.path("audio").path("frameCount").asInt());
        assertEquals("55BC801D", manifest.path("audio").path("frames").get(0).path("crc32").asText());
        assertTrue(manifest.path("createdAt").asText().endsWith("Z"));
        assertTrue(manifest.path("expiresAt").asText().endsWith("Z"));
        Instant createdAt = Instant.parse(manifest.path("createdAt").asText());
        Instant expiresAt = Instant.parse(manifest.path("expiresAt").asText());
        assertEquals(Duration.ofMinutes(5), Duration.between(createdAt, expiresAt));
        verify(deliveryMapper).insert(any(AnnouncementDelivery.class));
    }
}
