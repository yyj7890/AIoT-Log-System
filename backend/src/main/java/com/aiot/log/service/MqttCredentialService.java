package com.aiot.log.service;

import com.aiot.log.dto.MqttGlobalCredentialUpdateRequest;
import com.aiot.log.vo.MqttGlobalCredentialStatusVO;

public interface MqttCredentialService {
    MqttGlobalCredentialStatusVO getGlobalCredentialStatus();
    MqttGlobalCredentialStatusVO saveGlobalCredential(MqttGlobalCredentialUpdateRequest request);
    MqttGlobalCredentialStatusVO setAuthenticationEnabled(boolean enabled);
}
