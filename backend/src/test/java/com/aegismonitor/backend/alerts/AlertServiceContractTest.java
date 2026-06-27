package com.aegismonitor.backend.alerts;

import com.aegismonitor.backend.metrics.HostMetricPoint;
import java.util.List;

public final class AlertServiceContractTest {
    public static void main(String[] args) {
        generatesCpuAlertOnceAndAllowsAcknowledgement();
        generatesMemoryAndTcpAlertsFromTheSameMetricPoint();
        skipsExistingEventIdsWhenServiceRestartsWithPersistentRepository();
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

    private static void generatesMemoryAndTcpAlertsFromTheSameMetricPoint() {
        AlertService service = new AlertService();
        service.addRule(new AlertRule("rule_cpu_high", "CPU_HIGH", 80.0, "CRITICAL"));
        service.addRule(new AlertRule("rule_memory_high", "MEMORY_HIGH", 85.0, "WARNING"));
        service.addRule(new AlertRule("rule_tcp_connection_high", "TCP_CONNECTION_HIGH", 1000.0, "WARNING"));

        List<AlertEvent> generated = service.evaluate(
            new HostMetricPoint(
                "host_002",
                "2026-06-04T17:40:00+08:00",
                42.0,
                91.2,
                1208
            )
        );

        assertEquals(2, generated.size(), "memory and tcp alert count");

        AlertEvent memoryAlert = generated.get(0);
        assertEquals("alert_001", memoryAlert.eventId(), "memory event id");
        assertEquals("rule_memory_high", memoryAlert.ruleId(), "memory rule id");
        assertEquals("MEMORY_HIGH", memoryAlert.metricName(), "memory metric name");
        assertEquals("WARNING", memoryAlert.severity(), "memory severity");
        assertDoubleEquals(85.0, memoryAlert.threshold(), "memory threshold");
        assertDoubleEquals(91.2, memoryAlert.actualValue(), "memory actual value");

        AlertEvent tcpAlert = generated.get(1);
        assertEquals("alert_002", tcpAlert.eventId(), "tcp event id");
        assertEquals("rule_tcp_connection_high", tcpAlert.ruleId(), "tcp rule id");
        assertEquals("TCP_CONNECTION_HIGH", tcpAlert.metricName(), "tcp metric name");
        assertEquals("WARNING", tcpAlert.severity(), "tcp severity");
        assertDoubleEquals(1000.0, tcpAlert.threshold(), "tcp threshold");
        assertDoubleEquals(1208.0, tcpAlert.actualValue(), "tcp actual value");
    }

    private static void skipsExistingEventIdsWhenServiceRestartsWithPersistentRepository() {
        InMemoryAlertRepository repository = new InMemoryAlertRepository();
        repository.save(new AlertEvent(
            "alert_001",
            "rule_cpu_high",
            "host_old",
            "CPU_HIGH",
            "CRITICAL",
            80.0,
            93.0,
            "ACKED",
            "2026-06-04T17:00:00+08:00",
            "ops_001",
            "2026-06-04T17:01:00+08:00",
            "历史告警"
        ));

        AlertService service = new AlertService(repository);
        service.addRule(new AlertRule("rule_memory_high", "MEMORY_HIGH", 85.0, "WARNING"));

        List<AlertEvent> generated = service.evaluate(
            new HostMetricPoint(
                "host_003",
                "2026-06-04T17:45:00+08:00",
                30.0,
                90.0,
                128
            )
        );

        assertEquals(1, generated.size(), "new alert count after restart");
        assertEquals("alert_002", generated.get(0).eventId(), "event id skips existing id");
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