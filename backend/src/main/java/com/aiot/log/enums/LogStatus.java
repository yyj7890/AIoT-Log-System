package com.aiot.log.enums;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class LogStatus {

    public static final String PENDING = "PENDING";
    public static final String PROCESSING = "PROCESSING";
    public static final String RESOLVED = "RESOLVED";

    private static final Set<String> VALUES = new HashSet<String>(Arrays.asList(
            PENDING, PROCESSING, RESOLVED
    ));

    private LogStatus() {
    }

    public static boolean isValid(String status) {
        return status != null && VALUES.contains(status);
    }
}

