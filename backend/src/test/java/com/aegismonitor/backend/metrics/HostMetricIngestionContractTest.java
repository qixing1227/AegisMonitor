package com.aegismonitor.backend.metrics;

import java.util.Arrays;

public final class HostMetricIngestionContractTest {
    public static void main(String[] args) {
        storesNumericMetricsAndListeningPortSnapshotSeparately();
    }

    private static void storesNumericMetricsAndListeningPortSnapshotSeparately() {
        HostMetricIngestionService service = new HostMetricIngestionService();

        service.ingest(
            new HostMetricReport(
                "agt_001",
                "host_001",
                "2026-06-04T17:30:00+08:00",
                new CpuSample(42.5),
                new MemorySample(50.0),
                new TcpSample(128, Arrays.asList(80, 3306, 6379, 8080))
            )
        );

        HostMetricPoint point = service.latestMetricPoint("host_001");
        HostRuntimeSnapshot snapshot = service.latestRuntimeSnapshot("host_001");

        assertEquals("host_001", point.hostId(), "metric point host id");
        assertEquals("2026-06-04T17:30:00+08:00", point.reportedAt(), "metric reported time");
        assertDoubleEquals(42.5, point.cpuUsagePercent(), "cpu usage");
        assertDoubleEquals(50.0, point.memoryUsagePercent(), "memory usage");
        assertEquals(128, point.tcpConnectionCount(), "tcp connection count");

        assertEquals("host_001", snapshot.hostId(), "snapshot host id");
        assertEquals(Arrays.asList(80, 3306, 6379, 8080), snapshot.listeningPorts(), "listening ports");
        assertEquals("2026-06-04T17:30:00+08:00", snapshot.updatedAt(), "snapshot update time");
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
