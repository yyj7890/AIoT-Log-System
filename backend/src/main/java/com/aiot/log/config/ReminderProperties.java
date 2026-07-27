package com.aiot.log.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "reminder")
public class ReminderProperties {
    private boolean schedulerEnabled = false;

    public boolean isSchedulerEnabled() { return schedulerEnabled; }
    public void setSchedulerEnabled(boolean schedulerEnabled) { this.schedulerEnabled = schedulerEnabled; }
}
