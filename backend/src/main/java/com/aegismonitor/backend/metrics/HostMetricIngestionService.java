package com.aegismonitor.backend.metrics;

import com.aegismonitor.backend.error.ResourceNotFoundException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

public final class HostMetricIngestionService {
    private static final int DAILY_HISTORY_MAX_POINTS = 1200;
    private static final ZoneId HISTORY_DAY_ZONE = ZoneId.of("Asia/Shanghai");

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
                report.disk().usagePercent(),
                report.network().bytesSent(),
                report.network().bytesReceived(),
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
            .orElseThrow(() -> new ResourceNotFoundException("No metric point found for host " + hostId));
    }

    public List<HostMetricPoint> metricHistory(String hostId, MetricHistoryRange range) {
        return metricRepository
            .findLatest(hostId)
            .map(point -> {
                long latestEpochMillis = toEpochMillis(point.reportedAt());
                List<HostMetricPoint> points = metricRepository.findHistoryBetween(
                    hostId,
                    latestEpochMillis - range.windowMillis(),
                    latestEpochMillis + 1
                );
                return downsample(points, range.maxPoints());
            })
            .orElseGet(List::of);
    }

    public List<HostMetricPoint> metricHistoryForDay(String hostId, LocalDate date) {
        long startEpochMillis = date
            .atStartOfDay(HISTORY_DAY_ZONE)
            .toInstant()
            .toEpochMilli();
        long endEpochMillis = date
            .plusDays(1)
            .atStartOfDay(HISTORY_DAY_ZONE)
            .toInstant()
            .toEpochMilli();

        return downsample(
            metricRepository.findHistoryBetween(hostId, startEpochMillis, endEpochMillis),
            DAILY_HISTORY_MAX_POINTS
        );
    }

    public HostRuntimeSnapshot latestRuntimeSnapshot(String hostId) {
        HostRuntimeSnapshot snapshot = latestRuntimeSnapshots.get(hostId);
        if (snapshot == null) {
            throw new IllegalArgumentException("No runtime snapshot found for host " + hostId);
        }
        return snapshot;
    }

    private static List<HostMetricPoint> downsample(List<HostMetricPoint> points, int maxPoints) {
        if (points.size() <= maxPoints) {
            return points;
        }

        return IntStream
            .range(0, maxPoints)
            .mapToObj(index -> {
                int sourceIndex = (int) Math.round(index * (points.size() - 1.0) / (maxPoints - 1.0));
                return points.get(sourceIndex);
            })
            .toList();
    }

    private static long toEpochMillis(String reportedAt) {
        return OffsetDateTime.parse(reportedAt).toInstant().toEpochMilli();
    }
}
