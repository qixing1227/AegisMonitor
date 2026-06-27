package com.aegismonitor.backend.metrics;

public final class HostMetricReport {
    private final String agentId;
    private final String hostId;
    private final String reportedAt;
    private final CpuSample cpu;
    private final MemorySample memory;
    private final DiskSample disk;
    private final NetworkSample network;
    private final TcpSample tcp;

    public HostMetricReport(
        String agentId,
        String hostId,
        String reportedAt,
        CpuSample cpu,
        MemorySample memory,
        TcpSample tcp
    ) {
        this(
            agentId,
            hostId,
            reportedAt,
            cpu,
            memory,
            new DiskSample(0.0),
            new NetworkSample(0L, 0L),
            tcp
        );
    }

    public HostMetricReport(
        String agentId,
        String hostId,
        String reportedAt,
        CpuSample cpu,
        MemorySample memory,
        DiskSample disk,
        NetworkSample network,
        TcpSample tcp
    ) {
        this.agentId = agentId;
        this.hostId = hostId;
        this.reportedAt = reportedAt;
        this.cpu = cpu;
        this.memory = memory;
        this.disk = disk;
        this.network = network;
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

    public DiskSample disk() {
        return disk;
    }

    public NetworkSample network() {
        return network;
    }

    public TcpSample tcp() {
        return tcp;
    }
}
