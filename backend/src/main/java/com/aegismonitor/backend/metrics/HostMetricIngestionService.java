package com.aegismonitor.backend.metrics;

import java.util.HashMap;
import java.util.Map;

public final class HostMetricIngestionService {
    private final Map<String, HostMetricPoint> latestMetricPoints = new HashMap<>();
    private final Map<String, HostRuntimeSnapshot> latestRuntimeSnapshots = new HashMap<>();

    public void ingest(HostMetricReport report) {
        latestMetricPoints.put(
            report.hostId(),
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
        HostMetricPoint point = latestMetricPoints.get(hostId);
        if (point == null) {
            throw new IllegalArgumentException("No metric point found for host " + hostId);
        }
        return point;
    }

    public HostRuntimeSnapshot latestRuntimeSnapshot(String hostId) {
        HostRuntimeSnapshot snapshot = latestRuntimeSnapshots.get(hostId);
        if (snapshot == null) {
            throw new IllegalArgumentException("No runtime snapshot found for host " + hostId);
        }
        return snapshot;
    }
}
