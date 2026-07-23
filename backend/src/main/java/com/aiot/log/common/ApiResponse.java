package com.aiot.log.common;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "统一 API 响应")
public class ApiResponse<T> {

    @Schema(description = "业务状态码；200 表示成功", example = "200")
    private Integer code;

    @Schema(description = "响应说明", example = "success")
    private String message;

    @Schema(description = "业务数据；无返回数据时为 null")
    private T data;

    public ApiResponse() {
    }

    public ApiResponse(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<T>(200, "success", data);
    }

    public static <T> ApiResponse<T> success() {
        return new ApiResponse<T>(200, "success", null);
    }

    public static <T> ApiResponse<T> fail(Integer code, String message) {
        return new ApiResponse<T>(code, message, null);
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

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}

