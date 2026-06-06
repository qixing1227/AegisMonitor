DROP TABLE IF EXISTS alert_events;
DROP TABLE IF EXISTS service_instances;
DROP TABLE IF EXISTS agents;

CREATE TABLE agents (
    agent_id VARCHAR(64) PRIMARY KEY,
    host_id VARCHAR(64) NOT NULL,
    agent_secret VARCHAR(128) NOT NULL,
    hostname VARCHAR(128) NOT NULL,
    alias VARCHAR(128) NOT NULL,
    ip_address VARCHAR(64) NOT NULL,
    os_name VARCHAR(64) NOT NULL,
    os_version VARCHAR(64) NOT NULL,
    cpu_cores INT NOT NULL,
    memory_total_bytes BIGINT NOT NULL,
    boot_time VARCHAR(64) NOT NULL,
    agent_version VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    last_heartbeat_at VARCHAR(64),
    created_at VARCHAR(64) NOT NULL
);

CREATE TABLE alert_events (
    event_id VARCHAR(64) PRIMARY KEY,
    rule_id VARCHAR(64) NOT NULL,
    host_id VARCHAR(64) NOT NULL,
    metric_name VARCHAR(64) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    threshold_value DOUBLE NOT NULL,
    actual_value DOUBLE NOT NULL,
    status VARCHAR(32) NOT NULL,
    occurred_at VARCHAR(64) NOT NULL,
    acknowledged_by VARCHAR(64),
    acknowledged_at VARCHAR(64),
    ack_note VARCHAR(512)
);

CREATE TABLE service_instances (
    host_id VARCHAR(64) NOT NULL,
    service_name VARCHAR(128) NOT NULL,
    stack_type VARCHAR(64) NOT NULL,
    process_name VARCHAR(128) NOT NULL,
    pid INT NOT NULL,
    ports_json VARCHAR(512) NOT NULL,
    status VARCHAR(32) NOT NULL,
    command_line VARCHAR(1024) NOT NULL,
    last_seen_at VARCHAR(64) NOT NULL,
    PRIMARY KEY (host_id, stack_type, service_name)
);
