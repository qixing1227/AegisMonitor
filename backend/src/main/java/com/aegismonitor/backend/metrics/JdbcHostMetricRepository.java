package com.aegismonitor.backend.metrics;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

public final class JdbcHostMetricRepository implements HostMetricRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcHostMetricRepository(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void save(HostMetricPoint point) {
        long reportedAtEpochMillis = toEpochMillis(point.reportedAt());
        int updated = jdbcTemplate.update(
            """
            UPDATE host_metric_points
            SET reported_at = ?, cpu_usage_percent = ?, memory_usage_percent = ?, tcp_connection_count = ?
            WHERE host_id = ? AND reported_at_epoch_ms = ?
            """,
            point.reportedAt(),
            point.cpuUsagePercent(),
            point.memoryUsagePercent(),
            point.tcpConnectionCount(),
            point.hostId(),
            reportedAtEpochMillis
        );
        if (updated > 0) {
            return;
        }

        jdbcTemplate.update(
            """
            INSERT INTO host_metric_points (
                host_id,
                reported_at,
                reported_at_epoch_ms,
                cpu_usage_percent,
                memory_usage_percent,
                tcp_connection_count
            ) VALUES (?, ?, ?, ?, ?, ?)
            """,
            point.hostId(),
            point.reportedAt(),
            reportedAtEpochMillis,
            point.cpuUsagePercent(),
            point.memoryUsagePercent(),
            point.tcpConnectionCount()
        );
    }

    @Override
    public Optional<HostMetricPoint> findLatest(String hostId) {
        List<HostMetricPoint> points = jdbcTemplate.query(
            """
            SELECT host_id, reported_at, cpu_usage_percent, memory_usage_percent, tcp_connection_count
            FROM host_metric_points
            WHERE host_id = ?
            ORDER BY reported_at_epoch_ms DESC
            LIMIT 1
            """,
            rowMapper(),
            hostId
        );
        return points.stream().findFirst();
    }

    @Override
    public List<HostMetricPoint> findHistory(String hostId, long windowMillis, int maxPoints) {
        Long latestTimestamp = jdbcTemplate.queryForObject(
            "SELECT MAX(reported_at_epoch_ms) FROM host_metric_points WHERE host_id = ?",
            Long.class,
            hostId
        );
        if (latestTimestamp == null) {
            return List.of();
        }

        return jdbcTemplate.query(
            """
            SELECT host_id, reported_at, cpu_usage_percent, memory_usage_percent, tcp_connection_count
            FROM host_metric_points
            WHERE host_id = ? AND reported_at_epoch_ms >= ?
            ORDER BY reported_at_epoch_ms ASC
            LIMIT ?
            """,
            rowMapper(),
            hostId,
            latestTimestamp - windowMillis,
            maxPoints
        );
    }

    private static RowMapper<HostMetricPoint> rowMapper() {
        return (rs, rowNum) -> new HostMetricPoint(
            rs.getString("host_id"),
            rs.getString("reported_at"),
            rs.getDouble("cpu_usage_percent"),
            rs.getDouble("memory_usage_percent"),
            rs.getInt("tcp_connection_count")
        );
    }

    private static long toEpochMillis(String reportedAt) {
        return OffsetDateTime.parse(reportedAt).toInstant().toEpochMilli();
    }
}
