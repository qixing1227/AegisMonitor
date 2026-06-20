package com.aegismonitor.backend.metrics;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class HostMetricIngestionService {
    private final HostMetricRepository metricRepository;
    private final Map<String, HostRuntimeSnapshot> latestRuntimeSnapshots = new ConcurrentHashMap<>();

    public HostMetricIngestionService() {
        this(new InMemoryHostMetricRepository());
    }

    public HostMetricIngestionService(HostMetricRepository metricRepository) {
        this.metricRepository = metricRepository;
    }

    public void ingest(HostMetricReport report) {
        metricRepository.save(
            new HostMetricPoint(
                report.hostId(),
                report.reportedAt(),
                report.cpu().usagePercent(),
                report.memory().usagePercent(),
                report.tcp().connectionCount()
            )
        );

        latestRuntimeSnapshots.put(
            report.hostId(),
            new HostRuntimeSnapshot(
                report.hostId(),
                report.tcp().listeningPorts(),
                report.reportedAt()
            )
        );
    }

    public HostMetricPoint latestMetricPoint(String hostId) {
        return metricRepository
            .findLatest(hostId)
            .orElseThrow(() -> new IllegalArgumentException("No metric point found for host " + hostId));
    }

    public List<HostMetricPoint> metricHistory(String hostId, MetricHistoryRange range) {
        return metricRepository.findHistory(hostId, range.windowMillis(), range.maxPoints());
    }

    public HostRuntimeSnapshot latestRuntimeSnapshot(String hostId) {
        HostRuntimeSnapshot snapshot = latestRuntimeSnapshots.get(hostId);
        if (snapshot == null) {
            throw new IllegalArgumentException("No runtime snapshot found for host " + hostId);
        }
        return snapshot;
    }
}
