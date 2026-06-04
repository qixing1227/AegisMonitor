package com.aegismonitor.backend.agent;

public final class AgentRegistrationResult {
    private final String agentId;
    private final String hostId;
    private final String agentSecret;

    public AgentRegistrationResult(String agentId, String hostId, String agentSecret) {
        this.agentId = agentId;
        this.hostId = hostId;
        this.agentSecret = agentSecret;
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
}

