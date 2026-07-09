package com.aiot.log.enums;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class LogType {

    public static final String RUNNING = "RUNNING";
    public static final String ERROR = "ERROR";
    public static final String MAINTENANCE = "MAINTENANCE";
    public static final String INSPECTION = "INSPECTION";

    private static final Set<String> VALUES = new HashSet<String>(Arrays.asList(
            RUNNING, ERROR, MAINTENANCE, INSPECTION
    ));

    private LogType() {
    }

    public static boolean isValid(String logType) {
        return logType != null && VALUES.contains(logType);
    }
}

