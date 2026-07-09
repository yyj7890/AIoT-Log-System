package com.aiot.log.service;

import com.aiot.log.dto.AlertRuleRequest;
import com.aiot.log.vo.AlertRuleVO;

import java.util.List;

public interface AlertRuleService {

    List<AlertRuleVO> listRules(Long deviceId, Boolean enabled);

    AlertRuleVO createRule(AlertRuleRequest request);

    AlertRuleVO updateRule(Long id, AlertRuleRequest request);

    void deleteRule(Long id);
}
