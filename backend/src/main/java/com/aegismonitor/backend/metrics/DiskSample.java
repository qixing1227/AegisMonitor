package com.aegismonitor.backend.metrics;

public final class DiskSample {
    private final double usagePercent;

    public DiskSample(double usagePercent) {
        this.usagePercent = usagePercent;
    }

    public double usagePercent() {
        return usagePercent;
    }
}
