package com.aiot.log.controller;

import com.aiot.log.common.ApiResponse;
import com.aiot.log.config.MqttDeviceReportSubscriber;
import com.aiot.log.config.MqttProperties;
import com.aiot.log.vo.MqttStatusVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mqtt")
public class MqttController {

    private final MqttProperties mqttProperties;
    private final MqttDeviceReportSubscriber mqttSubscriber;

    public MqttController(MqttProperties mqttProperties, MqttDeviceReportSubscriber mqttSubscriber) {
        this.mqttProperties = mqttProperties;
        this.mqttSubscriber = mqttSubscriber;
    }

    @GetMapping("/status")
    public ApiResponse<MqttStatusVO> status() {
        MqttStatusVO vo = new MqttStatusVO();
        vo.setEnabled(mqttProperties.getEnabled());
        vo.setBrokerUrl(mqttProperties.getBrokerUrl());
        vo.setClientId(mqttProperties.getClientId());
        vo.setTopic(mqttProperties.getTopic());
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
}
