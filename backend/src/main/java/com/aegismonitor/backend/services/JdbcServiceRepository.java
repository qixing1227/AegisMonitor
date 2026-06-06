package com.aegismonitor.backend.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

public final class JdbcServiceRepository implements ServiceRepository {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JdbcServiceRepository(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void upsert(ServiceInstance service) {
        if (findByKey(service.hostId(), service.stackType(), service.serviceName()).isPresent()) {
            update(service);
            return;
        }
        insert(service);
    }

    @Override
    public List<ServiceInstance> findByHostId(String hostId) {
        return jdbcTemplate.query(
            selectSql() + " WHERE host_id = ? ORDER BY stack_type, service_name",
            rowMapper(),
            hostId
        );
    }

    @Override
    public Optional<ServiceInstance> findByKey(String hostId, String stackType, String serviceName) {
        List<ServiceInstance> results = jdbcTemplate.query(
            selectSql() + " WHERE host_id = ? AND stack_type = ? AND service_name = ?",
            rowMapper(),
            hostId,
            stackType,
            serviceName
        );
        return results.stream().findFirst();
    }

    private void insert(ServiceInstance service) {
        jdbcTemplate.update(
            """
            INSERT INTO service_instances (
                host_id,
                service_name,
                stack_type,
                process_name,
                pid,
                ports_json,
                status,
                command_line,
                last_seen_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            service.hostId(),
            service.serviceName(),
            service.stackType(),
            service.processName(),
            service.pid(),
            toPortsJson(service.ports()),
            service.status(),
            service.commandLine(),
            service.lastSeenAt()
        );
    }

    private void update(ServiceInstance service) {
        jdbcTemplate.update(
            """
            UPDATE service_instances
            SET
                process_name = ?,
                pid = ?,
                ports_json = ?,
                status = ?,
                command_line = ?,
                last_seen_at = ?
            WHERE host_id = ? AND stack_type = ? AND service_name = ?
            """,
            service.processName(),
            service.pid(),
            toPortsJson(service.ports()),
            service.status(),
            service.commandLine(),
            service.lastSeenAt(),
            service.hostId(),
            service.stackType(),
            service.serviceName()
        );
    }

    private static String selectSql() {
        return """
            SELECT
                host_id,
                service_name,
                stack_type,
                process_name,
                pid,
                ports_json,
                status,
                command_line,
                last_seen_at
            FROM service_instances
            """;
    }

    private RowMapper<ServiceInstance> rowMapper() {
        return (rs, rowNum) -> new ServiceInstance(
            rs.getString("host_id"),
            rs.getString("service_name"),
            rs.getString("stack_type"),
            rs.getString("process_name"),
            rs.getInt("pid"),
            fromPortsJson(rs.getString("ports_json")),
            rs.getString("status"),
            rs.getString("command_line"),
            rs.getString("last_seen_at")
        );
    }

    private String toPortsJson(List<Integer> ports) {
        try {
            return objectMapper.writeValueAsString(ports);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Could not serialize service ports", exception);
        }
    }

    private List<Integer> fromPortsJson(String portsJson) {
        try {
            return objectMapper.readValue(portsJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Could not parse service ports", exception);
        }
    }
}
