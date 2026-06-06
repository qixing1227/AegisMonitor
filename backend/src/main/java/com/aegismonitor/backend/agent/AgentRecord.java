package com.aegismonitor.backend.agent;

public record AgentRecord(
    String agentId,
    String hostId,
    String agentSecret,
    String hostname,
    String alias,
    String ipAddress,
    String osName,
    String osVersion,
    int cpuCores,
    long memoryTotalBytes,
    String bootTime,
    String agentVersion,
    String status,
    String lastHeartbeatAt,
    String createdAt
) {
}
