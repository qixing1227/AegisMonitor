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
        ensureMetricColumns();
    }

    @Override
    public void save(HostMetricPoint point) {
        long reportedAtEpochMillis = toEpochMillis(point.reportedAt());
        int updated = jdbcTemplate.update(
            """
            UPDATE host_metric_points
            SET reported_at = ?,
                cpu_usage_percent = ?,
                memory_usage_percent = ?,
                disk_usage_percent = ?,
                network_bytes_sent = ?,
                network_bytes_received = ?,
                tcp_connection_count = ?
            WHERE host_id = ? AND reported_at_epoch_ms = ?
            """,
            point.reportedAt(),
            point.cpuUsagePercent(),
            point.memoryUsagePercent(),
            point.diskUsagePercent(),
            point.networkBytesSent(),
            point.networkBytesReceived(),
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
                disk_usage_percent,
                network_bytes_sent,
                network_bytes_received,
                tcp_connection_count
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            point.hostId(),
            point.reportedAt(),
            reportedAtEpochMillis,
            point.cpuUsagePercent(),
            point.memoryUsagePercent(),
            point.diskUsagePercent(),
            point.networkBytesSent(),
            point.networkBytesReceived(),
            point.tcpConnectionCount()
        );
    }

    @Override
    public Optional<HostMetricPoint> findLatest(String hostId) {
        List<HostMetricPoint> points = jdbcTemplate.query(
            """
            SELECT host_id,
                   reported_at,
                   cpu_usage_percent,
                   memory_usage_percent,
                   disk_usage_percent,
                   network_bytes_sent,
                   network_bytes_received,
                   tcp_connection_count
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
    public List<HostMetricPoint> findHistoryBetween(
        String hostId,
        long startEpochMillisInclusive,
        long endEpochMillisExclusive
    ) {
        return jdbcTemplate.query(
            """
            SELECT host_id,
                   reported_at,
                   cpu_usage_percent,
                   memory_usage_percent,
                   disk_usage_percent,
                   network_bytes_sent,
                   network_bytes_received,
                   tcp_connection_count
            FROM host_metric_points
            WHERE host_id = ?
              AND reported_at_epoch_ms >= ?
              AND reported_at_epoch_ms < ?
            ORDER BY reported_at_epoch_ms ASC
            """,
            rowMapper(),
            hostId,
            startEpochMillisInclusive,
            endEpochMillisExclusive
        );
    }

    private void ensureMetricColumns() {
        ensureColumn("disk_usage_percent DOUBLE NOT NULL DEFAULT 0");
        ensureColumn("network_bytes_sent BIGINT NOT NULL DEFAULT 0");
        ensureColumn("network_bytes_received BIGINT NOT NULL DEFAULT 0");
    }

    private void ensureColumn(String columnDefinition) {
        try {
            jdbcTemplate.execute("ALTER TABLE host_metric_points ADD COLUMN " + columnDefinition);
        } catch (RuntimeException ignored) {
            // The column already exists in fresh schemas; old local MySQL databases need this migration once.
        }
    }

    private static RowMapper<HostMetricPoint> rowMapper() {
        return (rs, rowNum) -> new HostMetricPoint(
            rs.getString("host_id"),
            rs.getString("reported_at"),
            rs.getDouble("cpu_usage_percent"),
            rs.getDouble("memory_usage_percent"),
            rs.getDouble("disk_usage_percent"),
            rs.getLong("network_bytes_sent"),
            rs.getLong("network_bytes_received"),
            rs.getInt("tcp_connection_count")
        );
    }

    private static long toEpochMillis(String reportedAt) {
        return OffsetDateTime.parse(reportedAt).toInstant().toEpochMilli();
    }
}
