package com.aegismonitor.backend.metrics;

public final class MemorySample {
    private final double usagePercent;

    public MemorySample(double usagePercent) {
        this.usagePercent = usagePercent;
    }

    public double usagePercent() {
        return usagePercent;
    }
}

