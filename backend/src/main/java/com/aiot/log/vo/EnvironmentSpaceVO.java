package com.aiot.log.vo;
import java.math.BigDecimal; import java.time.LocalDateTime;
public class EnvironmentSpaceVO { public Long id; public String name; public String address; public BigDecimal latitude; public BigDecimal longitude; public Long primarySpeakerDeviceId; public String primarySpeakerName; public Boolean enabled; public boolean outdoorSourceConfigured; public LocalDateTime createdAt; public LocalDateTime updatedAt; }
