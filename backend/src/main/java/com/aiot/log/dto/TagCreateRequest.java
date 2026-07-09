package com.aiot.log.dto;

import jakarta.validation.constraints.NotBlank;

public class TagCreateRequest {

    @NotBlank(message = "标签名称不能为空")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

