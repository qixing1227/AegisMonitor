package com.aegismonitor.backend.agent;

import java.util.List;
import java.util.Optional;

public interface AgentRepository {
    void save(AgentRecord agent);

    Optional<AgentRecord> findByAgentId(String agentId);

    void updateHeartbeat(String agentId, String status, String lastHeartbeatAt);

    List<AgentRecord> findAll();
}
