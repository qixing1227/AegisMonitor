package com.aegismonitor.backend.agent;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class AgentRegistry {
    private final String registerToken;
    private final AgentRepository repository;
    private int nextHostNumber = 1;
    private int nextAgentNumber = 1;

    public AgentRegistry(String registerToken) {
        this(registerToken, new InMemoryAgentRepository());
    }

    public AgentRegistry(String registerToken, AgentRepository repository) {
        this.registerToken = registerToken;
        this.repository = repository;
    }

    public AgentRegistrationResult register(String token, AgentRegistrationRequest request) {
        if (!Objects.equals(registerToken, token)) {
            throw new IllegalArgumentException("Agent register token is invalid");
        }

        String hostId = formatId("host", nextHostNumber++);
        String agentId = formatId("agt", nextAgentNumber++);
        String agentSecret = UUID.randomUUID().toString();
        repository.save(
            new AgentRecord(
                agentId,
                hostId,
                agentSecret,
                request.hostname(),
                request.alias(),
                request.ipAddress(),
                request.osName(),
                request.osVersion(),
                request.cpuCores(),
                request.memoryTotalBytes(),
                request.bootTime(),
                request.agentVersion(),
                "ONLINE",
                null,
                OffsetDateTime.now().toString()
            )
        );
        return new AgentRegistrationResult(agentId, hostId, agentSecret);
    }

    public void heartbeat(AgentHeartbeatRequest request) {
        AgentRecord agent = repository
            .findByAgentId(request.agentId())
            .orElseThrow(() -> new IllegalArgumentException("Agent does not exist"));
        if (!Objects.equals(agent.hostId(), request.hostId())) {
            throw new IllegalArgumentException("Agent host does not match");
        }
        if (!Objects.equals(agent.agentSecret(), request.agentSecret())) {
            throw new IllegalArgumentException("Agent secret is invalid");
        }

        repository.updateHeartbeat(
            request.agentId(),
            request.status(),
            request.reportedAt()
        );
    }

    public AgentStatus getAgentStatus(String agentId) {
        AgentRecord agent = repository
            .findByAgentId(agentId)
            .orElseThrow(() -> new IllegalArgumentException("Agent does not exist"));
        return new AgentStatus(
            agent.agentId(),
            agent.hostId(),
            agent.status(),
            agent.lastHeartbeatAt()
        );
    }

    public List<AgentSummary> listAgents() {
        return repository.findAll()
            .stream()
            .map(AgentRegistry::toSummary)
            .toList();
    }

    private static AgentSummary toSummary(AgentRecord agent) {
        return new AgentSummary(
            agent.agentId(),
            agent.hostId(),
            agent.hostname(),
            agent.alias(),
            agent.ipAddress(),
            agent.osName(),
            agent.osVersion(),
            agent.cpuCores(),
            agent.memoryTotalBytes(),
            agent.agentVersion(),
            agent.status(),
            agent.lastHeartbeatAt()
        );
    }

    private static String formatId(String prefix, int value) {
        return String.format("%s_%03d", prefix, value);
    }
}
