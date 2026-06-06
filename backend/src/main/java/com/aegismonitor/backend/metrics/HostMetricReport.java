package com.aegismonitor.backend.metrics;

public final class HostMetricReport {
    private final String agentId;
    private final String hostId;
    private final String reportedAt;
    private final CpuSample cpu;
    private final MemorySample memory;
    private final TcpSample tcp;

    public HostMetricReport(
        String agentId,
        String hostId,
        String reportedAt,
        CpuSample cpu,
        MemorySample memory,
        TcpSample tcp
    ) {
        this.agentId = agentId;
        this.hostId = hostId;
        this.reportedAt = reportedAt;
        this.cpu = cpu;
        this.memory = memory;
        this.tcp = tcp;
    }

    public String agentId() {
        return agentId;
    }

    public String hostId() {
        return hostId;
    }

    public String reportedAt() {
        return reportedAt;
    }

    public CpuSample cpu() {
        return cpu;
    }

    public MemorySample memory() {
        return memory;
    }

    public TcpSample tcp() {
        return tcp;
    }
}

