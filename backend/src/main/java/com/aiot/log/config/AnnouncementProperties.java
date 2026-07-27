package com.aiot.log.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "announcement")
public class AnnouncementProperties {

    private boolean testEnabled = false;
    private String fixedTestResource = "classpath:announcements/fixed-test/manifest.json";

    public boolean isTestEnabled() {
        return testEnabled;
    }

    public void setTestEnabled(boolean testEnabled) {
        this.testEnabled = testEnabled;
    }

    public String getFixedTestResource() {
        return fixedTestResource;
    }

    public void setFixedTestResource(String fixedTestResource) {
        this.fixedTestResource = fixedTestResource;
    }
}
