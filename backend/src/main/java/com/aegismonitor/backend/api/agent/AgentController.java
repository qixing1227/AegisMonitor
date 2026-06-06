package com.aegismonitor.backend.api.agent;

import com.aegismonitor.backend.agent.AgentHeartbeatRequest;
import com.aegismonitor.backend.agent.AgentRegistrationRequest;
import com.aegismonitor.backend.agent.AgentRegistrationResult;
import com.aegismonitor.backend.agent.AgentRegistry;
import com.aegismonitor.backend.agent.AgentSummary;
import com.aegismonitor.backend.api.ApiResponse;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agents")
public class AgentController {
    private final AgentRegistry agentRegistry;

    public AgentController(AgentRegistry agentRegistry) {
        this.agentRegistry = agentRegistry;
    }

    @PostMapping("/register")
    public ApiResponse<AgentRegistrationHttpResponse> register(
        @RequestHeader("X-Agent-Register-Token") String registerToken,
        @RequestBody AgentRegistrationHttpRequest request
    ) {
        AgentRegistrationResult result = agentRegistry.register(
            registerToken,
            new AgentRegistrationRequest(
                request.hostname(),
                request.alias(),
                request.ipAddress(),
                request.osName(),
                request.osVersion(),
                request.cpuCores(),
                request.memoryTotalBytes(),
                request.bootTime(),
                request.agentVersion()
            )
        );

        return ApiResponse.ok(
            "registered",
            new AgentRegistrationHttpResponse(
                result.agentId(),
                result.hostId(),
                result.agentSecret(),
                true
            )
        );
    }

    @PostMapping("/heartbeat")
    public ApiResponse<AgentHeartbeatHttpResponse> heartbeat(
        @RequestHeader("X-Agent-Id") String agentId,
        @RequestHeader("X-Agent-Secret") String agentSecret,
        @RequestBody AgentHeartbeatHttpRequest request
    ) {
        agentRegistry.heartbeat(
            new AgentHeartbeatRequest(
                agentId,
                request.hostId(),
                agentSecret,
                request.status(),
                request.reportedAt()
            )
        );

        return ApiResponse.ok(
            "heartbeat accepted",
            new AgentHeartbeatHttpResponse(OffsetDateTime.now().toString())
        );
    }

    @GetMapping
    public ApiResponse<List<AgentDashboardHttpResponse>> listAgents() {
        return ApiResponse.ok(
            "agent list",
            agentRegistry.listAgents()
                .stream()
                .map(AgentDashboardHttpResponse::from)
                .toList()
        );
    }

    public record AgentRegistrationHttpRequest(
        String hostname,
        String alias,
        String ipAddress,
        String osName,
        String osVersion,
        int cpuCores,
        long memoryTotalBytes,
        String bootTime,
        String agentVersion,
        Map<String, Object> configSummary
    ) {
    }

    public record AgentRegistrationHttpResponse(
        String agentId,
        String hostId,
        String agentSecret,
        boolean approved
    ) {
    }

    public record AgentHeartbeatHttpRequest(
        String agentId,
        String hostId,
        String status,
        String reportedAt
    ) {
    }

    public record AgentHeartbeatHttpResponse(String serverTime) {
    }

    public record AgentDashboardHttpResponse(
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
        private static AgentDashboardHttpResponse from(AgentSummary summary) {
            return new AgentDashboardHttpResponse(
                summary.agentId(),
                summary.hostId(),
                summary.hostname(),
                summary.alias(),
                summary.ipAddress(),
                summary.osName(),
                summary.osVersion(),
                summary.cpuCores(),
                summary.memoryTotalBytes(),
                summary.agentVersion(),
                summary.status(),
                summary.lastHeartbeatAt()
            );
        }
    }
}
