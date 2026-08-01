package com.aiot.log.dto;
import jakarta.validation.constraints.NotBlank;
public class EnvironmentWeatherConfigRequest { @NotBlank public String apiHost; @NotBlank public String jwtKid; @NotBlank public String jwtProjectId; public String privateKeyPem; }
