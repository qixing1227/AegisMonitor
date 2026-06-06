package com.aegismonitor.backend.metrics;

public final class HostMetricPoint {
    private final String hostId;
    private final String reportedAt;
    private final double cpuUsagePercent;
    private final double memoryUsagePercent;
    private final int tcpConnectionCount;

    public HostMetricPoint(
        String hostId,
        String reportedAt,
        double cpuUsagePercent,
        double memoryUsagePercent,
        int tcpConnectionCount
    ) {
        this.hostId = hostId;
        this.reportedAt = reportedAt;
        this.cpuUsagePercent = cpuUsagePercent;
        this.memoryUsagePercent = memoryUsagePercent;
        this.tcpConnectionCount = tcpConnectionCount;
    }

    public String hostId() {
        return hostId;
    }

    public String reportedAt() {
        return reportedAt;
    }

    public double cpuUsagePercent() {
        return cpuUsagePercent;
    }

    public double memoryUsagePercent() {
        return memoryUsagePercent;
    }

    public int tcpConnectionCount() {
        return tcpConnectionCount;
    }
}

