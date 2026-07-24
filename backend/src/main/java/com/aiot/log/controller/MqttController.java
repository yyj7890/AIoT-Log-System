package com.aiot.log.controller;

import com.aiot.log.common.ApiResponse;
import com.aiot.log.config.MqttDeviceReportSubscriber;
import com.aiot.log.config.MqttProperties;
import com.aiot.log.dto.MqttGlobalCredentialUpdateRequest;
import com.aiot.log.dto.MqttRemoteCredentialUpdateRequest;
import com.aiot.log.exception.BusinessException;
import com.aiot.log.exception.ErrorCode;
import com.aiot.log.service.MqttCredentialService;
import com.aiot.log.service.RemoteMqttCredentialService;
import com.aiot.log.vo.MqttGlobalCredentialStatusVO;
import com.aiot.log.vo.MqttRemoteCredentialStatusVO;
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
    private final RemoteMqttCredentialService remoteMqttCredentialService;

    public MqttController(MqttProperties mqttProperties, MqttDeviceReportSubscriber mqttSubscriber,
                          MqttCredentialService mqttCredentialService,
                          RemoteMqttCredentialService remoteMqttCredentialService) {
        this.mqttProperties = mqttProperties;
        this.mqttSubscriber = mqttSubscriber;
        this.mqttCredentialService = mqttCredentialService;
        this.remoteMqttCredentialService = remoteMqttCredentialService;
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

    @GetMapping("/remote-credential")
    @Operation(summary = "获取远程 HiveMQ 凭证状态", description = "仅返回用户名和密码是否已配置，不返回密码")
    public ApiResponse<MqttRemoteCredentialStatusVO> remoteCredential() {
        ensureRemoteCredentialManagement();
        return ApiResponse.success(remoteMqttCredentialService.getStatus());
    }

    @PutMapping("/remote-credential")
    @Operation(summary = "保存远程 HiveMQ 凭证并重连", description = "凭证写入私有持久化文件，响应不返回密码")
    public ApiResponse<MqttRemoteCredentialStatusVO> saveRemoteCredential(
            @Valid @RequestBody MqttRemoteCredentialUpdateRequest request) {
        ensureRemoteCredentialManagement();
        MqttRemoteCredentialStatusVO status = remoteMqttCredentialService.save(request);
        mqttSubscriber.reconnect();
        return ApiResponse.success(status);
    }

    private void ensureLanCredentialManagement() {
        if (mqttProperties.isRemoteMode()) {
            throw new BusinessException(ErrorCode.MQTT_REMOTE_CREDENTIAL_READ_ONLY);
        }
    }

    private void ensureRemoteCredentialManagement() {
        if (!mqttProperties.isRemoteMode()) {
            throw new BusinessException(
                    ErrorCode.MQTT_REMOTE_CREDENTIAL_READ_ONLY,
                    "当前不是远程 MQTT 模式");
        }
    }
}
