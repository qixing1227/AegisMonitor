package com.aegismonitor.backend.agent;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class AgentRegistry {
    private final String registerToken;
    private final Map<String, RegisteredAgent> agents = new HashMap<>();
    private int nextHostNumber = 1;
    private int nextAgentNumber = 1;

    public AgentRegistry(String registerToken) {
        this.registerToken = registerToken;
    }

    public AgentRegistrationResult register(String token, AgentRegistrationRequest request) {
        if (!Objects.equals(registerToken, token)) {
            throw new IllegalArgumentException("Agent register token is invalid");
        }

        String hostId = formatId("host", nextHostNumber++);
        String agentId = formatId("agt", nextAgentNumber++);
        String agentSecret = UUID.randomUUID().toString();
        agents.put(agentId, new RegisteredAgent(agentId, hostId, agentSecret, "ONLINE", null));
        return new AgentRegistrationResult(agentId, hostId, agentSecret);
    }

    public void heartbeat(AgentHeartbeatRequest request) {
        RegisteredAgent agent = agents.get(request.agentId());
        if (agent == null) {
            throw new IllegalArgumentException("Agent does not exist");
        }
        if (!Objects.equals(agent.hostId, request.hostId())) {
            throw new IllegalArgumentException("Agent host does not match");
        }
        if (!Objects.equals(agent.agentSecret, request.agentSecret())) {
            throw new IllegalArgumentException("Agent secret is invalid");
        }

        agents.put(
            request.agentId(),
            new RegisteredAgent(
                request.agentId(),
                request.hostId(),
                request.agentSecret(),
                request.status(),
                request.reportedAt()
            )
        );
    }

    public AgentStatus getAgentStatus(String agentId) {
        RegisteredAgent agent = agents.get(agentId);
        if (agent == null) {
            throw new IllegalArgumentException("Agent does not exist");
        }
        return new AgentStatus(agent.agentId, agent.hostId, agent.status, agent.lastHeartbeatAt);
    }

    private static String formatId(String prefix, int value) {
        return String.format("%s_%03d", prefix, value);
    }

    private static final class RegisteredAgent {
        private final String agentId;
        private final String hostId;
        private final String agentSecret;
        private final String status;
        private final String lastHeartbeatAt;

        private RegisteredAgent(
            String agentId,
            String hostId,
            String agentSecret,
            String status,
            String lastHeartbeatAt
        ) {
            this.agentId = agentId;
            this.hostId = hostId;
            this.agentSecret = agentSecret;
            this.status = status;
            this.lastHeartbeatAt = lastHeartbeatAt;
        }
    }
}
