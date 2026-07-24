package com.aiot.log.service;

import com.aiot.log.dto.MqttRemoteCredentialUpdateRequest;
import com.aiot.log.vo.MqttRemoteCredentialStatusVO;

public interface RemoteMqttCredentialService {

    MqttRemoteCredentialStatusVO getStatus();

    MqttRemoteCredentialStatusVO save(MqttRemoteCredentialUpdateRequest request);
}
