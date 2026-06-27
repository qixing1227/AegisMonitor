package com.aegismonitor.backend.demo;

import com.aegismonitor.backend.agent.AgentRecord;
import com.aegismonitor.backend.agent.AgentRepository;
import com.aegismonitor.backend.alerts.AlertEvent;
import com.aegismonitor.backend.alerts.AlertRepository;
import com.aegismonitor.backend.metrics.CpuSample;
import com.aegismonitor.backend.metrics.HostMetricIngestionService;
import com.aegismonitor.backend.metrics.HostMetricReport;
import com.aegismonitor.backend.metrics.MemorySample;
import com.aegismonitor.backend.metrics.TcpSample;
import com.aegismonitor.backend.services.DiscoveredServiceReport;
import com.aegismonitor.backend.services.ServiceDiscoveryReport;
import com.aegismonitor.backend.services.ServiceInventory;
import java.util.ArrayList;
import java.util.List;

public final class DemoDataSeeder {
    private final AgentRepository agentRepository;
    private final ServiceInventory serviceInventory;
    private final AlertRepository alertRepository;
    private final HostMetricIngestionService hostMetricIngestionService;

    public DemoDataSeeder(
        AgentRepository agentRepository,
        ServiceInventory serviceInventory,
        AlertRepository alertRepository,
        HostMetricIngestionService hostMetricIngestionService
    ) {
        this.agentRepository = agentRepository;
        this.serviceInventory = serviceInventory;
        this.alertRepository = alertRepository;
        this.hostMetricIngestionService = hostMetricIngestionService;
    }

    public DemoSeedResult seed(
        int hostCount,
        boolean includeServices,
        boolean includeAlerts
    ) {
        List<AgentRecord> demoHosts = demoHosts().stream()
            .limit(Math.max(0, Math.min(hostCount, 3)))
            .toList();

        for (AgentRecord host : demoHosts) {
            if (agentRepository.findByAgentId(host.agentId()).isEmpty()) {
                agentRepository.save(host);
            }
            for (HostMetricReport metric : demoMetrics(host)) {
                hostMetricIngestionService.ingest(metric);
            }
        }

        int servicesCreated = includeServices ? seedServices(demoHosts) : 0;
        int alertsCreated = includeAlerts ? seedAlerts(demoHosts) : 0;

        return new DemoSeedResult(demoHosts.size(), servicesCreated, alertsCreated);
    }

    private static List<AgentRecord> demoHosts() {
        return List.of(
            new AgentRecord(
                "demo_agt_001",
                "demo_host_001",
                "demo-secret-001",
                "demo-web-01",
                "模拟 Web 主机",
                "10.0.0.11",
                "Windows Server",
                "2022",
                4,
                8589934592L,
                "2026-06-04T09:00:00+08:00",
                "0.1.0-demo",
                "ONLINE",
                "2026-06-04T17:30:00+08:00",
                "2026-06-04T10:00:01+08:00"
            ),
            new AgentRecord(
                "demo_agt_002",
                "demo_host_002",
                "demo-secret-002",
                "demo-app-01",
                "模拟应用主机",
                "10.0.0.12",
                "Ubuntu",
                "22.04",
                8,
                17179869184L,
                "2026-06-04T09:10:00+08:00",
                "0.1.0-demo",
                "ONLINE",
                "2026-06-04T17:30:00+08:00",
                "2026-06-04T10:00:02+08:00"
            ),
            new AgentRecord(
                "demo_agt_003",
                "demo_host_003",
                "demo-secret-003",
                "demo-db-01",
                "模拟数据库主机",
                "10.0.0.13",
                "CentOS",
                "7.9",
                8,
                34359738368L,
                "2026-06-04T09:20:00+08:00",
                "0.1.0-demo",
                "ONLINE",
                "2026-06-04T17:30:00+08:00",
                "2026-06-04T10:00:03+08:00"
            )
        );
    }

    private static List<HostMetricReport> demoMetrics(AgentRecord host) {
        DemoMetricProfile profile = demoMetricProfile(host.hostId());
        List<HostMetricReport> reports = new ArrayList<>();
        for (int index = 0; index <= 10; index++) {
            double progress = index / 10.0;
            double wave = Math.sin(index * 0.9);
            double cpu = index == 10
                ? profile.cpuUsagePercent()
                : roundOneDecimal(profile.cpuUsagePercent() - 8 + progress * 8 + wave * 3);
            double memory = index == 10
                ? profile.memoryUsagePercent()
                : roundOneDecimal(profile.memoryUsagePercent() - 4 + progress * 4 + wave);
            int tcpConnections = index == 10
                ? profile.tcpConnectionCount()
                : Math.max(0, profile.tcpConnectionCount() - 20 + index * 2 + (int) Math.round(wave * 4));

            reports.add(
                new HostMetricReport(
                    host.agentId(),
                    host.hostId(),
                    "2026-06-04T17:%02d:00+08:00".formatted(20 + index),
                    new CpuSample(cpu),
                    new MemorySample(memory),
                    new TcpSample(tcpConnections, profile.listeningPorts())
                )
            );
        }
        return List.copyOf(reports);
    }

    private static DemoMetricProfile demoMetricProfile(String hostId) {
        if ("demo_host_001".equals(hostId)) {
            return new DemoMetricProfile(36.8, 52.4, 41.5, 48, 125829120L, 536870912L, List.of(80));
        }
        if ("demo_host_002".equals(hostId)) {
            return new DemoMetricProfile(64.2, 68.7, 58.2, 96, 314572800L, 943718400L, List.of(8080, 6379));
        }
        return new DemoMetricProfile(93.5, 74.9, 76.4, 142, 734003200L, 2147483648L, List.of(3306));
    }

    private static double roundOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private int seedServices(List<AgentRecord> demoHosts) {
        int count = 0;
        for (AgentRecord host : demoHosts) {
            List<DiscoveredServiceReport> services = demoServices(host.hostId());
            count += serviceInventory.report(
                new ServiceDiscoveryReport(
                    host.agentId(),
                    host.hostId(),
                    "2026-06-04T17:30:00+08:00",
                    services
                )
            );
        }
        return count;
    }

    private static List<DiscoveredServiceReport> demoServices(String hostId) {
        if ("demo_host_001".equals(hostId)) {
            return List.of(
                new DiscoveredServiceReport(
                    "nginx",
                    "NGINX",
                    "nginx.exe",
                    8011,
                    List.of(80),
                    "RUNNING",
                    "nginx -g daemon off;"
                )
            );
        }
        if ("demo_host_002".equals(hostId)) {
            return List.of(
                new DiscoveredServiceReport(
                    "aegis-business-api",
                    "SPRING_BOOT",
                    "java.exe",
                    8012,
                    List.of(8080),
                    "RUNNING",
                    "java -jar aegis-business-api.jar"
                ),
                new DiscoveredServiceReport(
                    "redis",
                    "REDIS",
                    "redis-server.exe",
                    8013,
                    List.of(6379),
                    "RUNNING",
                    "redis-server redis.conf"
                )
            );
        }
        return List.of(
            new DiscoveredServiceReport(
                "mysql",
                "MYSQL",
                "mysqld.exe",
                8014,
                List.of(3306),
                "RUNNING",
                "mysqld --console"
            )
        );
    }

    private int seedAlerts(List<AgentRecord> demoHosts) {
        boolean hasDatabaseHost = demoHosts.stream()
            .anyMatch(host -> "demo_host_003".equals(host.hostId()));
        if (!hasDatabaseHost) {
            return 0;
        }

        AlertEvent event = new AlertEvent(
            "demo_alert_cpu_high",
            "rule_cpu_high",
            "demo_host_003",
            "CPU_HIGH",
            "CRITICAL",
            80.0,
            93.5,
            "OPEN",
            "2026-06-04T17:30:00+08:00",
            null,
            null,
            null
        );

        if (alertRepository.findByEventId(event.eventId()).isPresent()) {
            alertRepository.update(event);
        } else {
            alertRepository.save(event);
        }
        return 1;
    }

    private record DemoMetricProfile(
        double cpuUsagePercent,
        double memoryUsagePercent,
        double diskUsagePercent,
        int tcpConnectionCount,
        long networkBytesSent,
        long networkBytesReceived,
        List<Integer> listeningPorts
    ) {
    }
}
