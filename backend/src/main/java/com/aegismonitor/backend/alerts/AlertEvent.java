package com.aegismonitor.backend.alerts;

public record AlertEvent(
    String eventId,
    String ruleId,
    String hostId,
    String metricName,
    String severity,
    double threshold,
    double actualValue,
    String status,
    String occurredAt,
    String acknowledgedBy,
    String acknowledgedAt,
    String ackNote
) {
    public AlertEvent acknowledge(
        String acknowledgedBy,
        String acknowledgedAt,
        String ackNote
    ) {
        return new AlertEvent(
            eventId,
            ruleId,
            hostId,
            metricName,
            severity,
            threshold,
            actualValue,
            "ACKED",
            occurredAt,
            acknowledgedBy,
            acknowledgedAt,
            ackNote
        );
    }
}
