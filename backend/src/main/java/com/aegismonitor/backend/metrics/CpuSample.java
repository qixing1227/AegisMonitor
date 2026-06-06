package com.aegismonitor.backend.metrics;

public final class CpuSample {
    private final double usagePercent;

    public CpuSample(double usagePercent) {
        this.usagePercent = usagePercent;
    }

    public double usagePercent() {
        return usagePercent;
    }
}

