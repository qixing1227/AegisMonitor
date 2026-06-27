package com.aegismonitor.backend.alerts;

import com.aegismonitor.backend.metrics.HostMetricPoint;
import java.util.ArrayList;
import java.util.List;

public final class AlertService {
    private static final String CPU_HIGH = "CPU_HIGH";
    private static final String MEMORY_HIGH = "MEMORY_HIGH";
    private static final String TCP_CONNECTION_HIGH = "TCP_CONNECTION_HIGH";

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
                nextAvailableEventId(),
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
        return actualValue(rule, point) >= rule.threshold();
    }

    private static double actualValue(AlertRule rule, HostMetricPoint point) {
        return switch (rule.metricName()) {
            case CPU_HIGH -> point.cpuUsagePercent();
            case MEMORY_HIGH -> point.memoryUsagePercent();
            case TCP_CONNECTION_HIGH -> point.tcpConnectionCount();
            default -> throw new IllegalArgumentException("Unsupported alert metric " + rule.metricName());
        };
    }

    private String nextAvailableEventId() {
        String eventId;
        do {
            eventId = formatEventId(nextEventNumber++);
        } while (repository.findByEventId(eventId).isPresent());
        return eventId;
    }

    private static String formatEventId(int value) {
        return String.format("alert_%03d", value);
    }
}