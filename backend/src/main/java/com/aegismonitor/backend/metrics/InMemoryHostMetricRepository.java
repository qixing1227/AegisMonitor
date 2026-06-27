package com.aegismonitor.backend.metrics;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

public final class InMemoryHostMetricRepository implements HostMetricRepository {
    private final ConcurrentMap<String, ConcurrentNavigableMap<Long, HostMetricPoint>> pointsByHost =
        new ConcurrentHashMap<>();

    @Override
    public void save(HostMetricPoint point) {
        pointsByHost
            .computeIfAbsent(point.hostId(), ignored -> new ConcurrentSkipListMap<>())
            .put(toEpochMillis(point.reportedAt()), point);
    }

    @Override
    public Optional<HostMetricPoint> findLatest(String hostId) {
        ConcurrentNavigableMap<Long, HostMetricPoint> points = pointsByHost.get(hostId);
        return points == null || points.isEmpty()
            ? Optional.empty()
            : Optional.of(points.lastEntry().getValue());
    }

    @Override
    public List<HostMetricPoint> findHistoryBetween(
        String hostId,
        long startEpochMillisInclusive,
        long endEpochMillisExclusive
    ) {
        ConcurrentNavigableMap<Long, HostMetricPoint> points = pointsByHost.get(hostId);
        if (points == null || points.isEmpty()) {
            return List.of();
        }

        return List.copyOf(
            new ArrayList<>(
                points
                    .subMap(startEpochMillisInclusive, true, endEpochMillisExclusive, false)
                    .values()
            )
        );
    }

    private static long toEpochMillis(String reportedAt) {
        return OffsetDateTime.parse(reportedAt).toInstant().toEpochMilli();
    }
}
