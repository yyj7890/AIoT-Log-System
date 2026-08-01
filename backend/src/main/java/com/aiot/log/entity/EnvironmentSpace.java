package com.aiot.log.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("environment_spaces")
public class EnvironmentSpace {
    private Long id; private String name; private String address; private BigDecimal latitude; private BigDecimal longitude;
    private Long primarySpeakerDeviceId; private Boolean enabled; private LocalDateTime createdAt; private LocalDateTime updatedAt;
    public Long getId(){return id;} public void setId(Long value){id=value;} public String getName(){return name;} public void setName(String value){name=value;}
    public String getAddress(){return address;} public void setAddress(String value){address=value;} public BigDecimal getLatitude(){return latitude;} public void setLatitude(BigDecimal value){latitude=value;}
    public BigDecimal getLongitude(){return longitude;} public void setLongitude(BigDecimal value){longitude=value;} public Long getPrimarySpeakerDeviceId(){return primarySpeakerDeviceId;} public void setPrimarySpeakerDeviceId(Long value){primarySpeakerDeviceId=value;}
    public Boolean getEnabled(){return enabled;} public void setEnabled(Boolean value){enabled=value;} public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime value){createdAt=value;} public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime value){updatedAt=value;}
}
