package com.aegismonitor.backend.agent;

public final class AgentHeartbeatRequest {
    private final String agentId;
    private final String hostId;
    private final String agentSecret;
    private final String status;
    private final String reportedAt;

    public AgentHeartbeatRequest(
        String agentId,
        String hostId,
        String agentSecret,
        String status,
        String reportedAt
    ) {
        this.agentId = agentId;
        this.hostId = hostId;
        this.agentSecret = agentSecret;
        this.status = status;
        this.reportedAt = reportedAt;
    }

    public String agentId() {
        return agentId;
    }

    public String hostId() {
        return hostId;
    }

    public String agentSecret() {
        return agentSecret;
    }

    public String status() {
        return status;
    }

    public String reportedAt() {
        return reportedAt;
    }
}

