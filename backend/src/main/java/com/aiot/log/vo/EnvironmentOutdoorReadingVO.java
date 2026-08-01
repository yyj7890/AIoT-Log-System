package com.aiot.log.vo; import java.math.BigDecimal; import java.time.LocalDateTime;
public class EnvironmentOutdoorReadingVO { public BigDecimal temperature; public BigDecimal humidity; public String weatherText; public Integer aqi; public BigDecimal pm2p5; public String primaryPollutant; public LocalDateTime observedAt; }
