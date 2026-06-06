package com.aegismonitor.backend.agent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class InMemoryAgentRepository implements AgentRepository {
    private final Map<String, AgentRecord> agents = new LinkedHashMap<>();

    @Override
    public void save(AgentRecord agent) {
        agents.put(agent.agentId(), agent);
    }

    @Override
    public Optional<AgentRecord> findByAgentId(String agentId) {
        return Optional.ofNullable(agents.get(agentId));
    }

    @Override
    public void updateHeartbeat(String agentId, String status, String lastHeartbeatAt) {
        AgentRecord agent = agents.get(agentId);
        if (agent == null) {
            return;
        }
        agents.put(
            agentId,
            new AgentRecord(
                agent.agentId(),
                agent.hostId(),
                agent.agentSecret(),
                agent.hostname(),
                agent.alias(),
                agent.ipAddress(),
                agent.osName(),
                agent.osVersion(),
                agent.cpuCores(),
                agent.memoryTotalBytes(),
                agent.bootTime(),
                agent.agentVersion(),
                status,
                lastHeartbeatAt,
                agent.createdAt()
            )
        );
    }

    @Override
    public List<AgentRecord> findAll() {
        return new ArrayList<>(agents.values());
    }
}
