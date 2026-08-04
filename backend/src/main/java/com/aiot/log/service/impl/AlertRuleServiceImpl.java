package com.aiot.log.service.impl;

import com.aiot.log.dto.AlertRuleRequest;
import com.aiot.log.entity.AlertRule;
import com.aiot.log.entity.Device;
import com.aiot.log.enums.LogLevel;
import com.aiot.log.exception.BusinessException;
import com.aiot.log.exception.ErrorCode;
import com.aiot.log.mapper.AlertRuleMapper;
import com.aiot.log.mapper.DeviceMapper;
import com.aiot.log.service.AlertRuleService;
import com.aiot.log.vo.AlertRuleVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AlertRuleServiceImpl implements AlertRuleService {

    private static final Set<String> METRICS = new HashSet<String>(Arrays.asList(
            "temperature", "humidity", "pressure", "illuminance", "voltage", "signalStrength"
    ));
    private static final Set<String> OPERATORS = new HashSet<String>(Arrays.asList(
            "GT", "LT", "GTE", "LTE", "EQ"
    ));

    private final AlertRuleMapper alertRuleMapper;
    private final DeviceMapper deviceMapper;

    public AlertRuleServiceImpl(AlertRuleMapper alertRuleMapper, DeviceMapper deviceMapper) {
        this.alertRuleMapper = alertRuleMapper;
        this.deviceMapper = deviceMapper;
    }

    @Override
    public List<AlertRuleVO> listRules(Long deviceId, Boolean enabled) {
        LambdaQueryWrapper<AlertRule> wrapper = new LambdaQueryWrapper<AlertRule>()
                .orderByDesc(AlertRule::getCreatedAt);
        if (deviceId != null) {
            wrapper.eq(AlertRule::getDeviceId, deviceId);
        }
        if (enabled != null) {
            wrapper.eq(AlertRule::getEnabled, enabled);
        }

        List<AlertRule> rules = alertRuleMapper.selectList(wrapper);
        List<AlertRuleVO> result = new ArrayList<AlertRuleVO>();
        for (AlertRule rule : rules) {
            result.add(toVO(rule));
        }
        return result;
    }

    @Override
    public AlertRuleVO createRule(AlertRuleRequest request) {
        validateRequest(request);

        AlertRule rule = new AlertRule();
        fillRule(rule, request);
        alertRuleMapper.insert(rule);
        return toVO(alertRuleMapper.selectById(rule.getId()));
    }

    @Override
    public AlertRuleVO updateRule(Long id, AlertRuleRequest request) {
        validateRequest(request);
        AlertRule rule = getExistingRule(id);
        fillRule(rule, request);
        alertRuleMapper.updateById(rule);
        return toVO(alertRuleMapper.selectById(id));
    }

    @Override
    public void deleteRule(Long id) {
        getExistingRule(id);
        alertRuleMapper.deleteById(id);
    }

    private AlertRule getExistingRule(Long id) {
        AlertRule rule = alertRuleMapper.selectById(id);
        if (rule == null) {
            throw new BusinessException(ErrorCode.ALERT_RULE_NOT_FOUND);
        }
        return rule;
    }

    private void fillRule(AlertRule rule, AlertRuleRequest request) {
        rule.setName(request.getName());
        rule.setDeviceId(request.getDeviceId());
        rule.setMetric(request.getMetric());
        rule.setOperator(request.getOperator());
        rule.setThresholdValue(request.getThresholdValue());
        rule.setLevel(StringUtils.hasText(request.getLevel()) ? request.getLevel() : LogLevel.WARNING);
        rule.setEnabled(request.getEnabled() == null ? Boolean.TRUE : request.getEnabled());
    }

    private void validateRequest(AlertRuleRequest request) {
        if (request.getDeviceId() != null && deviceMapper.selectById(request.getDeviceId()) == null) {
            throw new BusinessException(ErrorCode.DEVICE_NOT_FOUND);
        }
        if (!METRICS.contains(request.getMetric())) {
            throw new BusinessException(ErrorCode.ALERT_METRIC_INVALID);
        }
        if (!OPERATORS.contains(request.getOperator())) {
            throw new BusinessException(ErrorCode.ALERT_OPERATOR_INVALID);
        }
        if (StringUtils.hasText(request.getLevel()) && !LogLevel.isValid(request.getLevel())) {
            throw new BusinessException(ErrorCode.ALERT_LEVEL_INVALID);
        }
    }

    private AlertRuleVO toVO(AlertRule rule) {
        AlertRuleVO vo = new AlertRuleVO();
        vo.setId(rule.getId());
        vo.setName(rule.getName());
        vo.setDeviceId(rule.getDeviceId());
        if (rule.getDeviceId() != null) {
            Device device = deviceMapper.selectById(rule.getDeviceId());
            if (device != null) {
                vo.setDeviceName(device.getName());
                vo.setDeviceCode(device.getDeviceCode());
            }
        }
        vo.setMetric(rule.getMetric());
        vo.setOperator(rule.getOperator());
        vo.setThresholdValue(rule.getThresholdValue());
        vo.setLevel(rule.getLevel());
        vo.setEnabled(rule.getEnabled());
        vo.setCreatedAt(rule.getCreatedAt());
        vo.setUpdatedAt(rule.getUpdatedAt());
        return vo;
    }
}
