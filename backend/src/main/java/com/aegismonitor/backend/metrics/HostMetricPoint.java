package com.aegismonitor.backend.metrics;

public final class HostMetricPoint {
    private final String hostId;
    private final String reportedAt;
    private final double cpuUsagePercent;
    private final double memoryUsagePercent;
    private final double diskUsagePercent;
    private final long networkBytesSent;
    private final long networkBytesReceived;
    private final int tcpConnectionCount;

    public HostMetricPoint(
        String hostId,
        String reportedAt,
        double cpuUsagePercent,
        double memoryUsagePercent,
        int tcpConnectionCount
    ) {
        this(
            hostId,
            reportedAt,
            cpuUsagePercent,
            memoryUsagePercent,
            0.0,
            0L,
            0L,
            tcpConnectionCount
        );
    }

    public HostMetricPoint(
        String hostId,
        String reportedAt,
        double cpuUsagePercent,
        double memoryUsagePercent,
        double diskUsagePercent,
        long networkBytesSent,
        long networkBytesReceived,
        int tcpConnectionCount
    ) {
        this.hostId = hostId;
        this.reportedAt = reportedAt;
        this.cpuUsagePercent = cpuUsagePercent;
        this.memoryUsagePercent = memoryUsagePercent;
        this.diskUsagePercent = diskUsagePercent;
        this.networkBytesSent = networkBytesSent;
        this.networkBytesReceived = networkBytesReceived;
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

    public double diskUsagePercent() {
        return diskUsagePercent;
    }

    public long networkBytesSent() {
        return networkBytesSent;
    }

    public long networkBytesReceived() {
        return networkBytesReceived;
    }

    public int tcpConnectionCount() {
        return tcpConnectionCount;
    }
}
