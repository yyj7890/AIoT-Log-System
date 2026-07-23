package com.aiot.log.controller;

import com.aiot.log.common.ApiResponse;
import com.aiot.log.config.MqttDeviceReportSubscriber;
import com.aiot.log.config.MqttProperties;
import com.aiot.log.dto.MqttGlobalCredentialUpdateRequest;
import com.aiot.log.exception.BusinessException;
import com.aiot.log.service.MqttCredentialService;
import com.aiot.log.vo.MqttGlobalCredentialStatusVO;
import com.aiot.log.vo.MqttStatusVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mqtt")
@Tag(name = "MQTT 状态", description = "MQTT连接、消息统计和局域网全局凭证管理")
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
    @Operation(summary = "获取 MQTT 状态", description = "远程模式会隐藏真实 Broker 地址")
    public ApiResponse<MqttStatusVO> status() {
        MqttStatusVO vo = new MqttStatusVO();
        vo.setEnabled(mqttProperties.getEnabled());
        vo.setMode(mqttProperties.isRemoteMode() ? "remote" : "lan");
        vo.setBrokerUrl(mqttProperties.isRemoteMode() ? "ssl://<private-remote-broker>" : mqttProperties.getBrokerUrl());
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
    @Operation(summary = "获取局域网全局凭证状态", description = "远程 HiveMQ 模式不允许调用")
    public ApiResponse<MqttGlobalCredentialStatusVO> globalCredential() {
        ensureLanCredentialManagement();
        return ApiResponse.success(mqttCredentialService.getGlobalCredentialStatus());
    }

    @PutMapping("/global-credential")
    @Operation(summary = "保存局域网全局凭证", description = "远程 HiveMQ 模式不允许调用；响应不会返回明文密码")
    public ApiResponse<MqttGlobalCredentialStatusVO> saveGlobalCredential(
            @Valid @RequestBody MqttGlobalCredentialUpdateRequest request) {
        ensureLanCredentialManagement();
        return ApiResponse.success(mqttCredentialService.saveGlobalCredential(request));
    }

    @PutMapping("/global-credential/authentication")
    @Operation(summary = "启用或停用局域网 MQTT 认证", description = "远程 HiveMQ 模式不允许调用")
    public ApiResponse<MqttGlobalCredentialStatusVO> setAuthenticationEnabled(
            @RequestParam boolean enabled) {
        ensureLanCredentialManagement();
        return ApiResponse.success(mqttCredentialService.setAuthenticationEnabled(enabled));
    }

    private void ensureLanCredentialManagement() {
        if (mqttProperties.isRemoteMode()) {
            throw new BusinessException(400, "远程 MQTT 模式仅使用部署环境中的私有凭证，不能在页面修改本地 Mosquitto 凭证");
        }
    }
}
