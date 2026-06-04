package com.aegismonitor.backend.agent;

public final class AgentStatus {
    private final String agentId;
    private final String hostId;
    private final String status;
    private final String lastHeartbeatAt;

    public AgentStatus(String agentId, String hostId, String status, String lastHeartbeatAt) {
        this.agentId = agentId;
        this.hostId = hostId;
        this.status = status;
        this.lastHeartbeatAt = lastHeartbeatAt;
    }

    public String agentId() {
        return agentId;
    }

    public String hostId() {
        return hostId;
    }

    public String status() {
        return status;
    }

    public String lastHeartbeatAt() {
        return lastHeartbeatAt;
    }
}

