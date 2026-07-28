package com.aiot.log.announcement;

import com.aiot.log.config.AnnouncementProperties;
import com.aiot.log.entity.AnnouncementDelivery;
import com.aiot.log.entity.Device;
import com.aiot.log.exception.BusinessException;
import com.aiot.log.exception.ErrorCode;
import com.aiot.log.mapper.AnnouncementDeliveryMapper;
import com.aiot.log.mapper.DeviceMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.CRC32;

@Service
public class FixedTestAnnouncementService {
    private static final String PROTOCOL = "aiot-announcement-v1";
    private static final int PRIORITY = 50;
    private final AnnouncementProperties properties;
    private final AnnouncementAudioSource audioSource;
    private final AnnouncementMqttGateway mqttGateway;
    private final DeviceMapper deviceMapper;
    private final AnnouncementDeliveryMapper deliveryMapper;
    private final ObjectMapper objectMapper;

    public FixedTestAnnouncementService(AnnouncementProperties properties, AnnouncementAudioSource audioSource,
                                        AnnouncementMqttGateway mqttGateway, DeviceMapper deviceMapper,
                                        AnnouncementDeliveryMapper deliveryMapper, ObjectMapper objectMapper) {
        this.properties = properties;
        this.audioSource = audioSource;
        this.mqttGateway = mqttGateway;
        this.deviceMapper = deviceMapper;
        this.deliveryMapper = deliveryMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public String publish(String deviceCode) {
        if (!properties.isTestEnabled()) throw new BusinessException(ErrorCode.ANNOUNCEMENT_TEST_DISABLED);
        Device device = deviceMapper.selectOne(new LambdaQueryWrapper<Device>().eq(Device::getDeviceCode, deviceCode));
        if (device == null) throw new BusinessException(ErrorCode.DEVICE_NOT_FOUND);
        AnnouncementAudio audio = audioSource.loadFixedTestAudio()
                .orElseThrow(() -> new BusinessException(ErrorCode.ANNOUNCEMENT_TEST_AUDIO_UNAVAILABLE));
        validateAudio(audio);

        LocalDateTime now = LocalDateTime.now();
        String taskId = "fixed-" + UUID.randomUUID();
        AnnouncementDelivery delivery = new AnnouncementDelivery();
        delivery.setTaskId(taskId);
        delivery.setDeviceId(device.getId());
        delivery.setDeviceCode(deviceCode);
        delivery.setPriority(PRIORITY);
        delivery.setExpiresAt(now.plusMinutes(5));
        delivery.setStatus("PUBLISHED");
        delivery.setCommandPublishedAt(now);
        deliveryMapper.insert(delivery);

        try {
            AnnouncementManifest manifest = buildManifest(taskId, deviceCode, now, delivery.getExpiresAt(), audio);
            mqttGateway.publish(commandTopic(deviceCode), objectMapper.writeValueAsBytes(manifest));
            for (AnnouncementAudioFrame frame : audio.frames()) {
                mqttGateway.publish(audioTopic(deviceCode, taskId, frame.index()), frame.payload());
            }
            return taskId;
        } catch (Exception exception) {
            delivery.setStatus("FAILED");
            delivery.setFailedAt(LocalDateTime.now());
            delivery.setFailureReason("mqtt_publish_failed");
            deliveryMapper.updateById(delivery);
            throw new BusinessException(ErrorCode.ANNOUNCEMENT_PUBLISH_FAILED);
        }
    }

    private AnnouncementManifest buildManifest(String taskId, String deviceCode, LocalDateTime createdAt,
                                                LocalDateTime expiresAt, AnnouncementAudio audio) {
        List<AnnouncementManifest.Frame> frames = new ArrayList<>();
        for (AnnouncementAudioFrame frame : audio.frames()) {
            frames.add(new AnnouncementManifest.Frame(frame.index(), frame.payload().length, crc32(frame.payload())));
        }
        return new AnnouncementManifest(PROTOCOL, taskId, deviceCode, PRIORITY,
                utcTimestamp(createdAt), utcTimestamp(expiresAt),
                new AnnouncementManifest.Audio("opus", 16000, 1, 60, frames.size(), frames));
    }

    private String utcTimestamp(LocalDateTime value) {
        return value.atZone(ZoneId.systemDefault()).toInstant().toString();
    }

    private void validateAudio(AnnouncementAudio audio) {
        if (!"opus".equals(audio.codec()) || audio.sampleRate() != 16000 || audio.channels() != 1
                || audio.frameDurationMs() != 60 || audio.frames() == null || audio.frames().isEmpty()
                || audio.frames().size() > 40) {
            throw new BusinessException(ErrorCode.ANNOUNCEMENT_TEST_AUDIO_UNAVAILABLE);
        }
        for (int i = 0; i < audio.frames().size(); i++) {
            AnnouncementAudioFrame frame = audio.frames().get(i);
            if (frame.index() != i || frame.payload() == null || frame.payload().length == 0
                    || frame.payload().length > 1500) {
                throw new BusinessException(ErrorCode.ANNOUNCEMENT_TEST_AUDIO_UNAVAILABLE);
            }
        }
    }

    private String crc32(byte[] payload) {
        CRC32 crc = new CRC32();
        crc.update(payload);
        return String.format("%08X", crc.getValue());
    }

    private String commandTopic(String deviceCode) {
        return "aiot/device/" + deviceCode + "/announcement/command";
    }

    private String audioTopic(String deviceCode, String taskId, int index) {
        return "aiot/device/" + deviceCode + "/announcement/audio/" + taskId + "/" + index;
    }
}
