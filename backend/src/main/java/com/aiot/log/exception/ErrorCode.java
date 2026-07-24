package com.aiot.log.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    REQUEST_VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "请求参数校验失败"),
    REQUEST_PARAMETER_INVALID(HttpStatus.BAD_REQUEST, "请求参数格式错误"),
    REQUEST_BODY_INVALID(HttpStatus.BAD_REQUEST, "请求体格式错误"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "请求方法不支持"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "请求资源不存在"),
    DATA_CONFLICT(HttpStatus.CONFLICT, "数据状态冲突"),

    DEVICE_NOT_FOUND(HttpStatus.NOT_FOUND, "设备不存在"),
    DEVICE_CODE_DUPLICATED(HttpStatus.CONFLICT, "设备编号已存在"),
    DEVICE_HAS_LOGS(HttpStatus.CONFLICT, "设备下存在日志，不能删除"),
    DEVICE_STATUS_INVALID(HttpStatus.BAD_REQUEST, "设备状态不合法"),
    DEVICE_MONITORING_MODE_INVALID(HttpStatus.BAD_REQUEST, "设备展示模式不合法"),

    LOG_NOT_FOUND(HttpStatus.NOT_FOUND, "日志不存在"),
    LOG_BATCH_ID_INVALID(HttpStatus.BAD_REQUEST, "日志编号不能为空"),
    LOG_BATCH_NOT_FOUND(HttpStatus.NOT_FOUND, "部分日志不存在"),
    LOG_TYPE_INVALID(HttpStatus.BAD_REQUEST, "日志类型不合法"),
    LOG_LEVEL_INVALID(HttpStatus.BAD_REQUEST, "日志等级不合法"),
    LOG_STATUS_INVALID(HttpStatus.BAD_REQUEST, "日志状态不合法"),
    LOG_SOURCE_INVALID(HttpStatus.BAD_REQUEST, "日志来源不合法"),

    TAG_NOT_FOUND(HttpStatus.NOT_FOUND, "标签不存在"),
    TAG_BATCH_NOT_FOUND(HttpStatus.NOT_FOUND, "部分标签不存在"),
    TAG_NAME_DUPLICATED(HttpStatus.CONFLICT, "标签名称已存在"),

    ALERT_RULE_NOT_FOUND(HttpStatus.NOT_FOUND, "告警规则不存在"),
    ALERT_METRIC_INVALID(HttpStatus.BAD_REQUEST, "告警指标不合法"),
    ALERT_OPERATOR_INVALID(HttpStatus.BAD_REQUEST, "比较符不合法"),
    ALERT_LEVEL_INVALID(HttpStatus.BAD_REQUEST, "告警等级不合法"),

    MQTT_REMOTE_CREDENTIAL_READ_ONLY(
            HttpStatus.BAD_REQUEST,
            "远程 MQTT 模式仅使用部署环境中的私有凭证，不能在页面修改本地 Mosquitto 凭证"),
    MQTT_REMOTE_CREDENTIAL_READ_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "无法读取远程 MQTT 私有凭证"),
    MQTT_REMOTE_CREDENTIAL_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "保存远程 MQTT 私有凭证失败"),
    MQTT_RECONNECT_FAILED(HttpStatus.BAD_GATEWAY, "远程 MQTT 使用新凭证重连失败"),
    MQTT_CREDENTIAL_REQUIRED(HttpStatus.BAD_REQUEST, "请先保存全局 MQTT 用户名和密码"),
    MQTT_CREDENTIAL_READ_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "无法读取 MQTT 凭证状态"),
    MQTT_CREDENTIAL_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "保存 MQTT 全局凭证失败"),
    MQTT_AUTH_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "更新 MQTT 认证开关失败"),

    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误");

    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }
}
