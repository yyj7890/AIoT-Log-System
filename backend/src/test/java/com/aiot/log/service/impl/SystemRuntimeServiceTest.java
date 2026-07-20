package com.aiot.log.service.impl;

import com.aiot.log.vo.SystemRuntimeVO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SystemRuntimeServiceTest {

    @Test
    void returnsBackendProcessUptimeInSeconds() {
        SystemRuntimeService service = new SystemRuntimeService(1_000L, () -> 6_550L);

        SystemRuntimeVO runtime = service.getRuntime();

        assertEquals(1_000L, runtime.getStartedAtEpochMillis());
        assertEquals(5L, runtime.getUptimeSeconds());
    }

    @Test
    void neverReturnsNegativeUptime() {
        SystemRuntimeService service = new SystemRuntimeService(10_000L, () -> 9_000L);

        assertEquals(0L, service.getRuntime().getUptimeSeconds());
    }
}
