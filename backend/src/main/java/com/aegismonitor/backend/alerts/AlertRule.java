package com.aegismonitor.backend.alerts;

public record AlertRule(
    String ruleId,
    String metricName,
    double threshold,
    String severity
) {
}
