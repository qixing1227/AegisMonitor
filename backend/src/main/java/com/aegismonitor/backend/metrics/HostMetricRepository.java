package com.aegismonitor.backend.metrics;

import java.util.List;
import java.util.Optional;

public interface HostMetricRepository {
    void save(HostMetricPoint point);

    Optional<HostMetricPoint> findLatest(String hostId);

    List<HostMetricPoint> findHistoryBetween(
        String hostId,
        long startEpochMillisInclusive,
        long endEpochMillisExclusive
    );
}
