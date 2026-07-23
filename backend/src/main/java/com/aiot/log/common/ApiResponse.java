package com.aiot.log.common;

import io.swagger.v3.oas.annotations.media.Schema;
import org.slf4j.MDC;

@Schema(description = "统一 API 响应")
public class ApiResponse<T> {

    @Schema(description = "业务状态码；200 表示成功", example = "200")
    private Integer code;

    @Schema(description = "响应说明", example = "success")
    private String message;

    @Schema(description = "稳定的业务错误码；成功时为 null", example = "DEVICE_NOT_FOUND")
    private String errorCode;

    @Schema(description = "请求追踪号，可与响应头 X-Trace-Id 及后端日志关联")
    private String traceId;

    @Schema(description = "业务数据；无返回数据时为 null")
    private T data;

    public ApiResponse() {
    }

    public ApiResponse(Integer code, String message, String errorCode, String traceId, T data) {
        this.code = code;
        this.message = message;
        this.errorCode = errorCode;
        this.traceId = traceId;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<T>(200, "success", null, currentTraceId(), data);
    }

    public static <T> ApiResponse<T> success() {
        return new ApiResponse<T>(200, "success", null, currentTraceId(), null);
    }

    public static <T> ApiResponse<T> fail(Integer code, String errorCode, String message) {
        return new ApiResponse<T>(code, message, errorCode, currentTraceId(), null);
    }

    private static String currentTraceId() {
        return MDC.get("traceId");
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}

