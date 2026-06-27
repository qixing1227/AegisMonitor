package com.aegismonitor.backend.api.metrics;

import com.aegismonitor.backend.agent.AgentRegistry;
import com.aegismonitor.backend.alerts.AlertService;
import com.aegismonitor.backend.api.ApiResponse;
import com.aegismonitor.backend.metrics.CpuSample;
import com.aegismonitor.backend.metrics.DiskSample;
import com.aegismonitor.backend.metrics.HostMetricIngestionService;
import com.aegismonitor.backend.metrics.HostMetricPoint;
import com.aegismonitor.backend.metrics.HostMetricReport;
import com.aegismonitor.backend.metrics.MemorySample;
import com.aegismonitor.backend.metrics.MetricHistoryRange;
import com.aegismonitor.backend.metrics.NetworkSample;
import com.aegismonitor.backend.metrics.TcpSample;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/metrics")
public class HostMetricController {
    private final HostMetricIngestionService ingestionService;
    private final AlertService alertService;
    private final AgentRegistry agentRegistry;

    public HostMetricController(
        HostMetricIngestionService ingestionService,
        AlertService alertService,
        AgentRegistry agentRegistry
    ) {
        this.ingestionService = ingestionService;
        this.alertService = alertService;
        this.agentRegistry = agentRegistry;
    }

    @PostMapping("/host")
    public ApiResponse<HostMetricIngestionHttpResponse> reportHostMetrics(
        @RequestHeader("X-Agent-Id") String agentId,
        @RequestHeader("X-Agent-Secret") String agentSecret,
        @RequestBody HostMetricHttpRequest request
    ) {
        agentRegistry.authenticate(agentId, request.hostId(), agentSecret);
        ingestionService.ingest(
            new HostMetricReport(
                agentId,
                request.hostId(),
                request.reportedAt(),
                new CpuSample(request.cpu().usagePercent()),
                new MemorySample(request.memory().usagePercent()),
                new DiskSample(maxDiskUsagePercent(request.disks())),
                new NetworkSample(
                    totalNetworkBytesSent(request.networks()),
                    totalNetworkBytesReceived(request.networks())
                ),
                new TcpSample(
                    request.tcp().connectionCount(),
                    request.tcp().listeningPorts()
                )
            )
        );
        int generatedAlertCount = alertService
            .evaluate(ingestionService.latestMetricPoint(request.hostId()))
            .size();

        return ApiResponse.ok(
            "metrics accepted",
            new HostMetricIngestionHttpResponse(true, generatedAlertCount)
        );
    }

    @GetMapping("/host/latest")
    public ApiResponse<LatestHostMetricHttpResponse> latestHostMetrics(
        @RequestParam String hostId
    ) {
        HostMetricPoint point = ingestionService.latestMetricPoint(hostId);

        return ApiResponse.ok(
            "latest host metrics",
            LatestHostMetricHttpResponse.from(point)
        );
    }

    @GetMapping("/host/history")
    public ApiResponse<HostMetricHistoryHttpResponse> hostMetricHistory(
        @RequestParam String hostId,
        @RequestParam(defaultValue = "10m") String range,
        @RequestParam(required = false) String date
    ) {
        if (date != null && !date.isBlank()) {
            LocalDate selectedDate;
            try {
                selectedDate = LocalDate.parse(date);
            } catch (DateTimeParseException exception) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported metric history date: " + date, exception);
            }

            List<HostMetricHistoryPointHttpResponse> points = ingestionService
                .metricHistoryForDay(hostId, selectedDate)
                .stream()
                .map(HostMetricHistoryPointHttpResponse::from)
                .toList();

            return ApiResponse.ok(
                "host metric history",
                new HostMetricHistoryHttpResponse(hostId, "day", selectedDate.toString(), points)
            );
        }

        MetricHistoryRange selectedRange;
        try {
            selectedRange = MetricHistoryRange.parse(range);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }

        List<HostMetricHistoryPointHttpResponse> points = ingestionService
            .metricHistory(hostId, selectedRange)
            .stream()
            .map(HostMetricHistoryPointHttpResponse::from)
            .toList();

        return ApiResponse.ok(
            "host metric history",
            new HostMetricHistoryHttpResponse(hostId, selectedRange.value(), null, points)
        );
    }

    private static double maxDiskUsagePercent(List<DiskMetricHttpRequest> disks) {
        if (disks == null || disks.isEmpty()) {
            return 0.0;
        }
        return disks.stream().mapToDouble(DiskMetricHttpRequest::usagePercent).max().orElse(0.0);
    }

    private static long totalNetworkBytesSent(List<NetworkMetricHttpRequest> networks) {
        if (networks == null || networks.isEmpty()) {
            return 0L;
        }
        return networks.stream().mapToLong(NetworkMetricHttpRequest::bytesSent).sum();
    }

    private static long totalNetworkBytesReceived(List<NetworkMetricHttpRequest> networks) {
        if (networks == null || networks.isEmpty()) {
            return 0L;
        }
        return networks.stream().mapToLong(NetworkMetricHttpRequest::bytesReceived).sum();
    }

    public record HostMetricHttpRequest(
        String agentId,
        String hostId,
        String reportedAt,
        CpuMetricHttpRequest cpu,
        MemoryMetricHttpRequest memory,
        List<DiskMetricHttpRequest> disks,
        List<NetworkMetricHttpRequest> networks,
        TcpMetricHttpRequest tcp
    ) {
    }

    public record CpuMetricHttpRequest(
        double usagePercent,
        List<Double> perCoreUsagePercent
    ) {
    }

    public record MemoryMetricHttpRequest(
        long totalBytes,
        long usedBytes,
        long availableBytes,
        double usagePercent
    ) {
    }

    public record DiskMetricHttpRequest(
        String mountPoint,
        long totalBytes,
        long usedBytes,
        long freeBytes,
        double usagePercent
    ) {
    }

    public record NetworkMetricHttpRequest(
        String interfaceName,
        long bytesSent,
        long bytesReceived,
        double sendRateBytesPerSecond,
        double receiveRateBytesPerSecond
    ) {
    }

    public record TcpMetricHttpRequest(
        int connectionCount,
        List<Integer> listeningPorts
    ) {
    }

    public record HostMetricIngestionHttpResponse(
        boolean written,
        int generatedAlertCount
    ) {
    }

    public record LatestHostMetricHttpResponse(
        String hostId,
        String reportedAt,
        double cpuUsagePercent,
        double memoryUsagePercent,
        double diskUsagePercent,
        long networkBytesSent,
        long networkBytesReceived,
        int tcpConnectionCount
    ) {
        static LatestHostMetricHttpResponse from(HostMetricPoint point) {
            return new LatestHostMetricHttpResponse(
                point.hostId(),
                point.reportedAt(),
                point.cpuUsagePercent(),
                point.memoryUsagePercent(),
                point.diskUsagePercent(),
                point.networkBytesSent(),
                point.networkBytesReceived(),
                point.tcpConnectionCount()
            );
        }
    }

    public record HostMetricHistoryHttpResponse(
        String hostId,
        String range,
        String date,
        List<HostMetricHistoryPointHttpResponse> points
    ) {
    }

    public record HostMetricHistoryPointHttpResponse(
        String reportedAt,
        double cpuUsagePercent,
        double memoryUsagePercent,
        double diskUsagePercent,
        long networkBytesSent,
        long networkBytesReceived,
        int tcpConnectionCount
    ) {
        static HostMetricHistoryPointHttpResponse from(HostMetricPoint point) {
            return new HostMetricHistoryPointHttpResponse(
                point.reportedAt(),
                point.cpuUsagePercent(),
                point.memoryUsagePercent(),
                point.diskUsagePercent(),
                point.networkBytesSent(),
                point.networkBytesReceived(),
                point.tcpConnectionCount()
            );
        }
    }
}
