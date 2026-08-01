package com.aiot.log.dto;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
public class EnvironmentSpaceRequest {
 @NotBlank(message="环境空间名称不能为空") private String name; private String address;
 @DecimalMin(value="-90") @DecimalMax(value="90") private BigDecimal latitude;
 @DecimalMin(value="-180") @DecimalMax(value="180") private BigDecimal longitude;
 private Long primarySpeakerDeviceId; private Boolean enabled;
 public String getName(){return name;} public void setName(String v){name=v;} public String getAddress(){return address;} public void setAddress(String v){address=v;}
 public BigDecimal getLatitude(){return latitude;} public void setLatitude(BigDecimal v){latitude=v;} public BigDecimal getLongitude(){return longitude;} public void setLongitude(BigDecimal v){longitude=v;}
 public Long getPrimarySpeakerDeviceId(){return primarySpeakerDeviceId;} public void setPrimarySpeakerDeviceId(Long v){primarySpeakerDeviceId=v;} public Boolean getEnabled(){return enabled;} public void setEnabled(Boolean v){enabled=v;}
}
