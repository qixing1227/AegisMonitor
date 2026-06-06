package com.aegismonitor.backend.agent;

public record AgentSummary(
    String agentId,
    String hostId,
    String hostname,
    String alias,
    String ipAddress,
    String osName,
    String osVersion,
    int cpuCores,
    long memoryTotalBytes,
    String agentVersion,
    String status,
    String lastHeartbeatAt
) {
}
