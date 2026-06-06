package com.aegismonitor.backend.alerts;

import com.aegismonitor.backend.metrics.HostMetricPoint;
import java.util.List;

public final class AlertServiceContractTest {
    public static void main(String[] args) {
        generatesCpuAlertOnceAndAllowsAcknowledgement();
    }

    private static void generatesCpuAlertOnceAndAllowsAcknowledgement() {
        AlertService service = new AlertService();
        service.addRule(new AlertRule("rule_cpu_high", "CPU_HIGH", 80.0, "CRITICAL"));

        List<AlertEvent> generated = service.evaluate(
            new HostMetricPoint(
                "host_001",
                "2026-06-04T17:30:00+08:00",
                91.5,
                50.0,
                128
            )
        );

        assertEquals(1, generated.size(), "first alert count");
        AlertEvent event = generated.get(0);
        assertEquals("alert_001", event.eventId(), "event id");
        assertEquals("rule_cpu_high", event.ruleId(), "rule id");
        assertEquals("host_001", event.hostId(), "host id");
        assertEquals("CPU_HIGH", event.metricName(), "metric name");
        assertEquals("CRITICAL", event.severity(), "severity");
        assertEquals("OPEN", event.status(), "status");
        assertDoubleEquals(80.0, event.threshold(), "threshold");
        assertDoubleEquals(91.5, event.actualValue(), "actual value");

        List<AlertEvent> duplicate = service.evaluate(
            new HostMetricPoint(
                "host_001",
                "2026-06-04T17:31:00+08:00",
                95.0,
                48.0,
                130
            )
        );
        assertEquals(0, duplicate.size(), "duplicate active alert count");

        AlertEvent acknowledged = service.acknowledge(
            "alert_001",
            "ops_001",
            "2026-06-04T17:32:00+08:00",
            "已通知现场同学排查 CPU 占用进程"
        );

        assertEquals("ACKED", acknowledged.status(), "ack status");
        assertEquals("ops_001", acknowledged.acknowledgedBy(), "ack user");
        assertEquals("2026-06-04T17:32:00+08:00", acknowledged.acknowledgedAt(), "ack time");
        assertEquals("已通知现场同学排查 CPU 占用进程", acknowledged.ackNote(), "ack note");
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void assertDoubleEquals(double expected, double actual, String label) {
        if (Math.abs(expected - actual) > 0.000001) {
            throw new AssertionError(label + " expected <" + expected + "> but was <" + actual + ">");
        }
    }
}
