package com.aegismonitor.backend.alerts;

import com.aegismonitor.backend.metrics.HostMetricPoint;
import java.util.ArrayList;
import java.util.List;

public final class AlertService {
    private final List<AlertRule> rules = new ArrayList<>();
    private final AlertRepository repository;
    private int nextEventNumber = 1;

    public AlertService() {
        this(new InMemoryAlertRepository());
    }

    public AlertService(AlertRepository repository) {
        this.repository = repository;
    }

    public void addRule(AlertRule rule) {
        rules.add(rule);
    }

    public List<AlertEvent> evaluate(HostMetricPoint point) {
        List<AlertEvent> generated = new ArrayList<>();
        for (AlertRule rule : rules) {
            if (!isTriggered(rule, point)) {
                continue;
            }

            if (repository.findOpenByHostIdAndRuleId(point.hostId(), rule.ruleId()).isPresent()) {
                continue;
            }

            AlertEvent event = new AlertEvent(
                formatEventId(nextEventNumber++),
                rule.ruleId(),
                point.hostId(),
                rule.metricName(),
                rule.severity(),
                rule.threshold(),
                actualValue(rule, point),
                "OPEN",
                point.reportedAt(),
                null,
                null,
                null
            );
            repository.save(event);
            generated.add(event);
        }
        return generated;
    }

    public AlertEvent acknowledge(
        String eventId,
        String acknowledgedBy,
        String acknowledgedAt,
        String ackNote
    ) {
        AlertEvent event = repository
            .findByEventId(eventId)
            .orElseThrow(() -> new IllegalArgumentException("Alert event does not exist"));

        AlertEvent acknowledged = event.acknowledge(acknowledgedBy, acknowledgedAt, ackNote);
        repository.update(acknowledged);
        return acknowledged;
    }

    public List<AlertEvent> listEvents() {
        return repository.findAll();
    }

    private static boolean isTriggered(AlertRule rule, HostMetricPoint point) {
        return "CPU_HIGH".equals(rule.metricName())
            && point.cpuUsagePercent() >= rule.threshold();
    }

    private static double actualValue(AlertRule rule, HostMetricPoint point) {
        if ("CPU_HIGH".equals(rule.metricName())) {
            return point.cpuUsagePercent();
        }
        throw new IllegalArgumentException("Unsupported alert metric " + rule.metricName());
    }

    private static String formatEventId(int value) {
        return String.format("alert_%03d", value);
    }
}
