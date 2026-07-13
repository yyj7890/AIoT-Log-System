package com.aiot.log.controller;

import com.aiot.log.common.ApiResponse;
import com.aiot.log.config.MqttDeviceReportSubscriber;
import com.aiot.log.config.MqttProperties;
import com.aiot.log.dto.MqttGlobalCredentialUpdateRequest;
import com.aiot.log.service.MqttCredentialService;
import com.aiot.log.vo.MqttGlobalCredentialStatusVO;
import com.aiot.log.vo.MqttStatusVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mqtt")
public class MqttController {

    private final MqttProperties mqttProperties;
    private final MqttDeviceReportSubscriber mqttSubscriber;
    private final MqttCredentialService mqttCredentialService;

    public MqttController(MqttProperties mqttProperties, MqttDeviceReportSubscriber mqttSubscriber,
                          MqttCredentialService mqttCredentialService) {
        this.mqttProperties = mqttProperties;
        this.mqttSubscriber = mqttSubscriber;
        this.mqttCredentialService = mqttCredentialService;
    }

    @GetMapping("/status")
    public ApiResponse<MqttStatusVO> status() {
        MqttStatusVO vo = new MqttStatusVO();
        vo.setEnabled(mqttProperties.getEnabled());
        vo.setBrokerUrl(mqttProperties.getBrokerUrl());
        vo.setClientId(mqttProperties.getClientId());
        vo.setTopic(mqttProperties.getTopic());
        vo.setLogTopic(mqttProperties.getLogTopic());
        vo.setQos(mqttProperties.getQos());
        vo.setConnected(mqttSubscriber.isConnected());
        vo.setLastConnectedAt(mqttSubscriber.getLastConnectedAt());
        vo.setLastDisconnectedAt(mqttSubscriber.getLastDisconnectedAt());
        vo.setLastMessageAt(mqttSubscriber.getLastMessageAt());
        vo.setLastMessageTopic(mqttSubscriber.getLastMessageTopic());
        vo.setReceivedCount(mqttSubscriber.getReceivedCount());
        vo.setHandledCount(mqttSubscriber.getHandledCount());
        vo.setFailedCount(mqttSubscriber.getFailedCount());
        vo.setLastError(mqttSubscriber.getLastError());
        return ApiResponse.success(vo);
    }

    @GetMapping("/global-credential")
    public ApiResponse<MqttGlobalCredentialStatusVO> globalCredential() {
        return ApiResponse.success(mqttCredentialService.getGlobalCredentialStatus());
    }

    @PutMapping("/global-credential")
    public ApiResponse<MqttGlobalCredentialStatusVO> saveGlobalCredential(
            @Valid @RequestBody MqttGlobalCredentialUpdateRequest request) {
        return ApiResponse.success(mqttCredentialService.saveGlobalCredential(request));
    }

    @PutMapping("/global-credential/authentication")
    public ApiResponse<MqttGlobalCredentialStatusVO> setAuthenticationEnabled(
            @RequestParam boolean enabled) {
        return ApiResponse.success(mqttCredentialService.setAuthenticationEnabled(enabled));
    }
}
