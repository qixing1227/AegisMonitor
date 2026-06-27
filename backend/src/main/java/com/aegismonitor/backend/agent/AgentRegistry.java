package com.aegismonitor.backend.agent;

import com.aegismonitor.backend.error.AgentAccessException;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class AgentRegistry {
    private static final Duration HEARTBEAT_OFFLINE_THRESHOLD = Duration.ofSeconds(60);

    private final String registerToken;
    private final AgentRepository repository;
    private final Clock clock;
    private int nextHostNumber = 1;
    private int nextAgentNumber = 1;

    public AgentRegistry(String registerToken) {
        this(registerToken, new InMemoryAgentRepository(), Clock.systemDefaultZone());
    }

    public AgentRegistry(String registerToken, AgentRepository repository) {
        this(registerToken, repository, Clock.systemDefaultZone());
    }

    public AgentRegistry(String registerToken, AgentRepository repository, Clock clock) {
        this.registerToken = registerToken;
        this.repository = repository;
        this.clock = clock;
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
                OffsetDateTime.now(clock).toString()
            )
        );
        return new AgentRegistrationResult(agentId, hostId, agentSecret);
    }

    public void authenticate(String agentId, String hostId, String agentSecret) {
        AgentRecord agent = repository
            .findByAgentId(agentId)
            .orElseThrow(AgentAccessException::unauthorized);
        if (!Objects.equals(agent.agentSecret(), agentSecret)) {
            throw AgentAccessException.unauthorized();
        }
        if (!Objects.equals(agent.hostId(), hostId)) {
            throw AgentAccessException.forbidden();
        }
    }

    public void heartbeat(AgentHeartbeatRequest request) {
        authenticate(request.agentId(), request.hostId(), request.agentSecret());

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
            effectiveStatus(agent),
            agent.lastHeartbeatAt()
        );
    }

    public List<AgentSummary> listAgents() {
        return repository.findAll()
            .stream()
            .map(this::toSummary)
            .toList();
    }

    private AgentSummary toSummary(AgentRecord agent) {
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
            effectiveStatus(agent),
            agent.lastHeartbeatAt()
        );
    }

    private String effectiveStatus(AgentRecord agent) {
        if (isDemoAgent(agent)) {
            return agent.status();
        }
        if (!"ONLINE".equals(agent.status()) || agent.lastHeartbeatAt() == null || agent.lastHeartbeatAt().isBlank()) {
            return agent.status();
        }

        try {
            OffsetDateTime lastHeartbeatAt = OffsetDateTime.parse(agent.lastHeartbeatAt());
            OffsetDateTime now = OffsetDateTime.now(clock);
            return lastHeartbeatAt.plus(HEARTBEAT_OFFLINE_THRESHOLD).isBefore(now)
                ? "OFFLINE"
                : "ONLINE";
        } catch (RuntimeException ignored) {
            return agent.status();
        }
    }

    private static boolean isDemoAgent(AgentRecord agent) {
        return agent.agentId().startsWith("demo_")
            || agent.hostId().startsWith("demo_")
            || agent.agentVersion().endsWith("-demo");
    }

    private static String formatId(String prefix, int value) {
        return String.format("%s_%03d", prefix, value);
    }
}
