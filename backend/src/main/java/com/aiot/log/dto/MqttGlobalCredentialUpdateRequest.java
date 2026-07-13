package com.aiot.log.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class MqttGlobalCredentialUpdateRequest {

    @NotBlank(message = "MQTT 用户名不能为空")
    @Pattern(regexp = "[A-Za-z0-9_-]{3,64}", message = "MQTT 用户名仅可包含字母、数字、下划线和短横线")
    private String username;

    @NotBlank(message = "MQTT 密码不能为空")
    @Size(min = 8, max = 128, message = "MQTT 密码长度应为 8 到 128 个字符")
    private String password;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
