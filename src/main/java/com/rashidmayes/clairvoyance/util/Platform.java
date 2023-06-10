package com.rashidmayes.clairvoyance.util;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;

public final class Platform {

    public MemoryUsage getHeapMemoryUsage() {
        return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
    }

    public MemoryUsage getNonHeapMemoryUsage() {
        return ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage();
    }

    public long getCurrentThreadCpuTime() {
        return ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
    }

    public long getCurrentThreadUserTime() {
        return ManagementFactory.getThreadMXBean().getCurrentThreadUserTime();
    }

    public long getPeakThreadCount() {
        return ManagementFactory.getThreadMXBean().getPeakThreadCount();
    }

    public long getStartTime() {
        return ManagementFactory.getRuntimeMXBean().getStartTime();
    }

    public long getUptime() {
        return ManagementFactory.getRuntimeMXBean().getUptime();
    }

    public int getAvailableProcessors() {
        return ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
    }

}
