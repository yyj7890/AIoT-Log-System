package com.aiot.log.service.impl;

import com.aiot.log.config.MqttProperties;
import com.aiot.log.dto.MqttRemoteCredentialUpdateRequest;
import com.aiot.log.exception.BusinessException;
import com.aiot.log.exception.ErrorCode;
import com.aiot.log.service.RemoteMqttCredentialService;
import com.aiot.log.vo.MqttRemoteCredentialStatusVO;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Properties;

@Service
public class RemoteMqttCredentialServiceImpl implements RemoteMqttCredentialService {

    private static final String USERNAME_KEY = "mqtt.username";
    private static final String PASSWORD_KEY = "mqtt.password";

    private final MqttProperties mqttProperties;
    private final Path credentialFile;
    private volatile boolean runtimeOverrideEnabled;

    public RemoteMqttCredentialServiceImpl(MqttProperties mqttProperties) {
        this.mqttProperties = mqttProperties;
        this.credentialFile = resolveCredentialFile();
    }

    @PostConstruct
    public void loadSavedCredential() {
        if (!mqttProperties.isRemoteMode() || !Files.isRegularFile(credentialFile)) {
            return;
        }
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(credentialFile)) {
            properties.load(input);
            String username = properties.getProperty(USERNAME_KEY, "");
            String password = properties.getProperty(PASSWORD_KEY, "");
            if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
                mqttProperties.setUsername(username);
                mqttProperties.setPassword(password);
                runtimeOverrideEnabled = true;
            }
        } catch (IOException exception) {
            throw new BusinessException(
                    ErrorCode.MQTT_REMOTE_CREDENTIAL_READ_FAILED,
                    ErrorCode.MQTT_REMOTE_CREDENTIAL_READ_FAILED.getMessage(),
                    exception);
        }
    }

    @Override
    public MqttRemoteCredentialStatusVO getStatus() {
        MqttRemoteCredentialStatusVO status = new MqttRemoteCredentialStatusVO();
        status.setUsername(mqttProperties.getUsername());
        status.setPasswordConfigured(StringUtils.hasText(mqttProperties.getPassword()));
        status.setRuntimeOverrideEnabled(runtimeOverrideEnabled);
        return status;
    }

    @Override
    public synchronized MqttRemoteCredentialStatusVO save(MqttRemoteCredentialUpdateRequest request) {
        Properties properties = new Properties();
        properties.setProperty(USERNAME_KEY, request.getUsername());
        properties.setProperty(PASSWORD_KEY, request.getPassword());
        Path parent = credentialFile.toAbsolutePath().getParent();
        Path temporary = credentialFile.resolveSibling(credentialFile.getFileName() + ".tmp");
        try {
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (OutputStream output = Files.newOutputStream(temporary)) {
                properties.store(output, "Private remote MQTT credential. Do not commit or share.");
            }
            secureFile(temporary);
            try {
                Files.move(temporary, credentialFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, credentialFile, StandardCopyOption.REPLACE_EXISTING);
            }
            secureFile(credentialFile);
            mqttProperties.setUsername(request.getUsername());
            mqttProperties.setPassword(request.getPassword());
            runtimeOverrideEnabled = true;
            return getStatus();
        } catch (IOException exception) {
            throw new BusinessException(
                    ErrorCode.MQTT_REMOTE_CREDENTIAL_SAVE_FAILED,
                    ErrorCode.MQTT_REMOTE_CREDENTIAL_SAVE_FAILED.getMessage(),
                    exception);
        }
    }

    private void secureFile(Path file) {
        try {
            Files.setPosixFilePermissions(file, EnumSet.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE));
        } catch (UnsupportedOperationException | IOException ignored) {
            // Windows does not expose POSIX permissions; the ignored private directory remains the boundary.
        }
    }

    private Path resolveCredentialFile() {
        String configured = mqttProperties.getRemoteCredentialFile();
        if (StringUtils.hasText(configured)) {
            return Path.of(configured);
        }
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        Path projectRoot = current.getFileName().toString().equals("backend") ? current.getParent() : current;
        return projectRoot.resolve("config").resolve("hivemq-remote-credentials.properties");
    }
}
