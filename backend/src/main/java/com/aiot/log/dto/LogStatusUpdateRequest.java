package com.aiot.log.dto;

import jakarta.validation.constraints.NotBlank;

public class LogStatusUpdateRequest {

    @NotBlank(message = "日志状态不能为空")
    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

