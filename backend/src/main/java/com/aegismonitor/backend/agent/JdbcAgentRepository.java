package com.aegismonitor.backend.agent;

import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

public final class JdbcAgentRepository implements AgentRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcAgentRepository(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void save(AgentRecord agent) {
        jdbcTemplate.update(
            """
            INSERT INTO agents (
                agent_id,
                host_id,
                agent_secret,
                hostname,
                alias,
                ip_address,
                os_name,
                os_version,
                cpu_cores,
                memory_total_bytes,
                boot_time,
                agent_version,
                status,
                last_heartbeat_at,
                created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            agent.agentId(),
            agent.hostId(),
            agent.agentSecret(),
            agent.hostname(),
            agent.alias(),
            agent.ipAddress(),
            agent.osName(),
            agent.osVersion(),
            agent.cpuCores(),
            agent.memoryTotalBytes(),
            agent.bootTime(),
            agent.agentVersion(),
            agent.status(),
            agent.lastHeartbeatAt(),
            agent.createdAt()
        );
    }

    @Override
    public Optional<AgentRecord> findByAgentId(String agentId) {
        List<AgentRecord> results = jdbcTemplate.query(
            """
            SELECT
                agent_id,
                host_id,
                agent_secret,
                hostname,
                alias,
                ip_address,
                os_name,
                os_version,
                cpu_cores,
                memory_total_bytes,
                boot_time,
                agent_version,
                status,
                last_heartbeat_at,
                created_at
            FROM agents
            WHERE agent_id = ?
            """,
            rowMapper(),
            agentId
        );
        return results.stream().findFirst();
    }

    @Override
    public void updateHeartbeat(String agentId, String status, String lastHeartbeatAt) {
        jdbcTemplate.update(
            """
            UPDATE agents
            SET status = ?, last_heartbeat_at = ?
            WHERE agent_id = ?
            """,
            status,
            lastHeartbeatAt,
            agentId
        );
    }

    @Override
    public List<AgentRecord> findAll() {
        return jdbcTemplate.query(
            """
            SELECT
                agent_id,
                host_id,
                agent_secret,
                hostname,
                alias,
                ip_address,
                os_name,
                os_version,
                cpu_cores,
                memory_total_bytes,
                boot_time,
                agent_version,
                status,
                last_heartbeat_at,
                created_at
            FROM agents
            ORDER BY created_at, agent_id
            """,
            rowMapper()
        );
    }

    private static RowMapper<AgentRecord> rowMapper() {
        return (rs, rowNum) -> new AgentRecord(
            rs.getString("agent_id"),
            rs.getString("host_id"),
            rs.getString("agent_secret"),
            rs.getString("hostname"),
            rs.getString("alias"),
            rs.getString("ip_address"),
            rs.getString("os_name"),
            rs.getString("os_version"),
            rs.getInt("cpu_cores"),
            rs.getLong("memory_total_bytes"),
            rs.getString("boot_time"),
            rs.getString("agent_version"),
            rs.getString("status"),
            rs.getString("last_heartbeat_at"),
            rs.getString("created_at")
        );
    }
}
