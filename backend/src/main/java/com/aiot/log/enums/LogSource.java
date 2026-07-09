package com.aiot.log.enums;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class LogSource {

    public static final String MANUAL = "MANUAL";
    public static final String DEVICE = "DEVICE";
    public static final String SYSTEM = "SYSTEM";

    private static final Set<String> VALUES = new HashSet<String>(Arrays.asList(
            MANUAL, DEVICE, SYSTEM
    ));

    private LogSource() {
    }

    public static boolean isValid(String source) {
        return source != null && VALUES.contains(source);
    }
}
