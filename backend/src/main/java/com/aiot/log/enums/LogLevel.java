package com.aiot.log.enums;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class LogLevel {

    public static final String INFO = "INFO";
    public static final String WARNING = "WARNING";
    public static final String ERROR = "ERROR";

    private static final Set<String> VALUES = new HashSet<String>(Arrays.asList(
            INFO, WARNING, ERROR
    ));

    private LogLevel() {
    }

    public static boolean isValid(String level) {
        return level != null && VALUES.contains(level);
    }
}

