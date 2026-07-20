package com.aiot.log.service.impl;

import com.aiot.log.vo.SystemRuntimeVO;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.util.function.LongSupplier;

@Service
public class SystemRuntimeService {

    private final long startedAtEpochMillis;
    private final LongSupplier currentTimeMillis;

    public SystemRuntimeService() {
        this(ManagementFactory.getRuntimeMXBean().getStartTime(), System::currentTimeMillis);
    }

    SystemRuntimeService(long startedAtEpochMillis, LongSupplier currentTimeMillis) {
        this.startedAtEpochMillis = startedAtEpochMillis;
        this.currentTimeMillis = currentTimeMillis;
    }

    public SystemRuntimeVO getRuntime() {
        long uptimeMillis = Math.max(0L, currentTimeMillis.getAsLong() - startedAtEpochMillis);
        SystemRuntimeVO runtime = new SystemRuntimeVO();
        runtime.setStartedAtEpochMillis(startedAtEpochMillis);
        runtime.setUptimeSeconds(uptimeMillis / 1000L);
        return runtime;
    }
}
