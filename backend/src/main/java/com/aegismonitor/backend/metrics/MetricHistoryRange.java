package com.aegismonitor.backend.metrics;

import java.time.Duration;
import java.util.Arrays;

public enum MetricHistoryRange {
    TEN_MINUTES("10m", Duration.ofMinutes(10), 180),
    THIRTY_MINUTES("30m", Duration.ofMinutes(30), 420),
    ONE_HOUR("1h", Duration.ofHours(1), 800);

    private final String value;
    private final Duration duration;
    private final int maxPoints;

    MetricHistoryRange(String value, Duration duration, int maxPoints) {
        this.value = value;
        this.duration = duration;
        this.maxPoints = maxPoints;
    }

    public String value() {
        return value;
    }

    public long windowMillis() {
        return duration.toMillis();
    }

    public int maxPoints() {
        return maxPoints;
    }

    public static MetricHistoryRange parse(String value) {
        return Arrays.stream(values())
            .filter(range -> range.value.equals(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unsupported metric history range: " + value));
    }
}
