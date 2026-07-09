package com.aiot.log.enums;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class DeviceStatus {

    public static final String NORMAL = "NORMAL";
    public static final String ABNORMAL = "ABNORMAL";
    public static final String OFFLINE = "OFFLINE";
    public static final String MAINTENANCE = "MAINTENANCE";

    private static final Set<String> VALUES = new HashSet<String>(Arrays.asList(
            NORMAL, ABNORMAL, OFFLINE, MAINTENANCE
    ));

    private DeviceStatus() {
    }

    public static boolean isValid(String status) {
        return status != null && VALUES.contains(status);
    }
}

