package com.aegismonitor.backend.api.metrics;

import com.aegismonitor.backend.alerts.AlertService;
import com.aegismonitor.backend.api.ApiResponse;
import com.aegismonitor.backend.metrics.CpuSample;
import com.aegismonitor.backend.metrics.HostMetricIngestionService;
import com.aegismonitor.backend.metrics.HostMetricPoint;
import com.aegismonitor.backend.metrics.HostMetricReport;
import com.aegismonitor.backend.metrics.MemorySample;
import com.aegismonitor.backend.metrics.TcpSample;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/metrics")
public class HostMetricController {
    private final HostMetricIngestionService ingestionService;
    private final AlertService alertService;

    public HostMetricController(
        HostMetricIngestionService ingestionService,
        AlertService alertService
    ) {
        this.ingestionService = ingestionService;
        this.alertService = alertService;
    }

    @PostMapping("/host")
    public ApiResponse<HostMetricIngestionHttpResponse> reportHostMetrics(
        @RequestHeader("X-Agent-Id") String agentId,
        @RequestHeader("X-Agent-Secret") String agentSecret,
        @RequestBody HostMetricHttpRequest request
    ) {
        ingestionService.ingest(
            new HostMetricReport(
                agentId,
                request.hostId(),
                request.reportedAt(),
                new CpuSample(request.cpu().usagePercent()),
                new MemorySample(request.memory().usagePercent()),
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
            new LatestHostMetricHttpResponse(
                point.hostId(),
                point.reportedAt(),
                point.cpuUsagePercent(),
                point.memoryUsagePercent(),
                point.tcpConnectionCount()
            )
        );
    }

    public record HostMetricHttpRequest(
        String agentId,
        String hostId,
        String reportedAt,
        CpuMetricHttpRequest cpu,
        MemoryMetricHttpRequest memory,
        List<Map<String, Object>> disks,
        List<Map<String, Object>> networks,
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
        int tcpConnectionCount
    ) {
    }
}
