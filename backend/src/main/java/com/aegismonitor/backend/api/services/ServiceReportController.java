package com.aegismonitor.backend.api.services;

import com.aegismonitor.backend.api.ApiResponse;
import com.aegismonitor.backend.services.DiscoveredServiceReport;
import com.aegismonitor.backend.services.ServiceDiscoveryReport;
import com.aegismonitor.backend.services.ServiceInventory;
import com.aegismonitor.backend.services.ServiceInstance;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/services")
public class ServiceReportController {
    private final ServiceInventory serviceInventory;

    public ServiceReportController(ServiceInventory serviceInventory) {
        this.serviceInventory = serviceInventory;
    }

    @PostMapping("/report")
    public ApiResponse<ServiceReportHttpResponse> reportServices(
        @RequestHeader("X-Agent-Id") String agentId,
        @RequestHeader("X-Agent-Secret") String agentSecret,
        @RequestBody ServiceReportHttpRequest request
    ) {
        int upsertedCount = serviceInventory.report(
            new ServiceDiscoveryReport(
                agentId,
                request.hostId(),
                request.reportedAt(),
                request.services().stream()
                    .map(ServiceReportController::toDomain)
                    .toList()
            )
        );

        return ApiResponse.ok(
            "services accepted",
            new ServiceReportHttpResponse(upsertedCount)
        );
    }

    @GetMapping
    public ApiResponse<List<ServiceDashboardHttpResponse>> listServices(@RequestParam String hostId) {
        return ApiResponse.ok(
            "service list",
            serviceInventory.latestServices(hostId)
                .stream()
                .map(ServiceDashboardHttpResponse::from)
                .toList()
        );
    }

    private static DiscoveredServiceReport toDomain(DiscoveredServiceHttpRequest service) {
        return new DiscoveredServiceReport(
            service.serviceName(),
            service.stackType(),
            service.processName(),
            service.pid(),
            service.ports(),
            service.status(),
            service.commandLine()
        );
    }

    public record ServiceReportHttpRequest(
        String agentId,
        String hostId,
        String reportedAt,
        List<DiscoveredServiceHttpRequest> services
    ) {
    }

    public record DiscoveredServiceHttpRequest(
        String serviceName,
        String stackType,
        String processName,
        int pid,
        List<Integer> ports,
        String status,
        String commandLine
    ) {
    }

    public record ServiceReportHttpResponse(int upsertedCount) {
    }

    public record ServiceDashboardHttpResponse(
        String hostId,
        String serviceName,
        String stackType,
        String processName,
        int pid,
        List<Integer> ports,
        String status,
        String commandLine,
        String lastSeenAt
    ) {
        private static ServiceDashboardHttpResponse from(ServiceInstance service) {
            return new ServiceDashboardHttpResponse(
                service.hostId(),
                service.serviceName(),
                service.stackType(),
                service.processName(),
                service.pid(),
                service.ports(),
                service.status(),
                service.commandLine(),
                service.lastSeenAt()
            );
        }
    }
}
