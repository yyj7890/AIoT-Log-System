package com.aiot.log.service.impl;

import com.aiot.log.config.MqttProperties;
import com.aiot.log.dto.MqttRemoteCredentialUpdateRequest;
import com.aiot.log.vo.MqttRemoteCredentialStatusVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemoteMqttCredentialServiceImplTest {

    @TempDir
    Path tempDirectory;

    @Test
    void savesPrivateCredentialAndLoadsItAfterRestartWithoutReturningPassword() throws Exception {
        Path credentialFile = tempDirectory.resolve("hivemq-remote-credentials.properties");
        MqttProperties firstProperties = remoteProperties(credentialFile);
        RemoteMqttCredentialServiceImpl firstService = new RemoteMqttCredentialServiceImpl(firstProperties);

        MqttRemoteCredentialUpdateRequest request = new MqttRemoteCredentialUpdateRequest();
        request.setUsername("hivemq-user");
        request.setPassword("secret-password");
        MqttRemoteCredentialStatusVO saved = firstService.save(request);

        assertEquals("hivemq-user", saved.getUsername());
        assertTrue(saved.getPasswordConfigured());
        assertTrue(saved.getRuntimeOverrideEnabled());
        assertTrue(Files.readString(credentialFile).contains("mqtt.password=secret-password"));

        MqttProperties restartedProperties = remoteProperties(credentialFile);
        restartedProperties.setUsername("environment-user");
        restartedProperties.setPassword("environment-password");
        RemoteMqttCredentialServiceImpl restartedService = new RemoteMqttCredentialServiceImpl(restartedProperties);
        restartedService.loadSavedCredential();

        assertEquals("hivemq-user", restartedProperties.getUsername());
        assertEquals("secret-password", restartedProperties.getPassword());
        assertTrue(restartedService.getStatus().getRuntimeOverrideEnabled());
    }

    @Test
    void doesNotLoadRuntimeOverrideOutsideRemoteMode() throws Exception {
        Path credentialFile = tempDirectory.resolve("hivemq-remote-credentials.properties");
        Files.writeString(credentialFile, "mqtt.username=saved\nmqtt.password=saved-password\n");
        MqttProperties properties = new MqttProperties();
        properties.setMode("lan");
        properties.setRemoteCredentialFile(credentialFile.toString());

        RemoteMqttCredentialServiceImpl service = new RemoteMqttCredentialServiceImpl(properties);
        service.loadSavedCredential();

        assertFalse(service.getStatus().getRuntimeOverrideEnabled());
    }

    private MqttProperties remoteProperties(Path credentialFile) {
        MqttProperties properties = new MqttProperties();
        properties.setMode("remote");
        properties.setRemoteCredentialFile(credentialFile.toString());
        return properties;
    }
}
