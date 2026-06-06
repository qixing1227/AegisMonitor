package com.aegismonitor.backend.alerts;

import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

public final class JdbcAlertRepository implements AlertRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcAlertRepository(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void save(AlertEvent event) {
        jdbcTemplate.update(
            """
            INSERT INTO alert_events (
                event_id,
                rule_id,
                host_id,
                metric_name,
                severity,
                threshold_value,
                actual_value,
                status,
                occurred_at,
                acknowledged_by,
                acknowledged_at,
                ack_note
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            event.eventId(),
            event.ruleId(),
            event.hostId(),
            event.metricName(),
            event.severity(),
            event.threshold(),
            event.actualValue(),
            event.status(),
            event.occurredAt(),
            event.acknowledgedBy(),
            event.acknowledgedAt(),
            event.ackNote()
        );
    }

    @Override
    public Optional<AlertEvent> findByEventId(String eventId) {
        List<AlertEvent> results = jdbcTemplate.query(
            selectSql() + " WHERE event_id = ?",
            rowMapper(),
            eventId
        );
        return results.stream().findFirst();
    }

    @Override
    public Optional<AlertEvent> findOpenByHostIdAndRuleId(String hostId, String ruleId) {
        List<AlertEvent> results = jdbcTemplate.query(
            selectSql() + " WHERE host_id = ? AND rule_id = ? AND status = 'OPEN'",
            rowMapper(),
            hostId,
            ruleId
        );
        return results.stream().findFirst();
    }

    @Override
    public void update(AlertEvent event) {
        jdbcTemplate.update(
            """
            UPDATE alert_events
            SET
                status = ?,
                acknowledged_by = ?,
                acknowledged_at = ?,
                ack_note = ?
            WHERE event_id = ?
            """,
            event.status(),
            event.acknowledgedBy(),
            event.acknowledgedAt(),
            event.ackNote(),
            event.eventId()
        );
    }

    @Override
    public List<AlertEvent> findAll() {
        return jdbcTemplate.query(
            selectSql() + " ORDER BY occurred_at, event_id",
            rowMapper()
        );
    }

    private static String selectSql() {
        return """
            SELECT
                event_id,
                rule_id,
                host_id,
                metric_name,
                severity,
                threshold_value,
                actual_value,
                status,
                occurred_at,
                acknowledged_by,
                acknowledged_at,
                ack_note
            FROM alert_events
            """;
    }

    private static RowMapper<AlertEvent> rowMapper() {
        return (rs, rowNum) -> new AlertEvent(
            rs.getString("event_id"),
            rs.getString("rule_id"),
            rs.getString("host_id"),
            rs.getString("metric_name"),
            rs.getString("severity"),
            rs.getDouble("threshold_value"),
            rs.getDouble("actual_value"),
            rs.getString("status"),
            rs.getString("occurred_at"),
            rs.getString("acknowledged_by"),
            rs.getString("acknowledged_at"),
            rs.getString("ack_note")
        );
    }
}
