package com.aegismonitor.backend;

import com.aegismonitor.backend.agent.AgentRepository;
import com.aegismonitor.backend.agent.AgentRegistry;
import com.aegismonitor.backend.agent.JdbcAgentRepository;
import com.aegismonitor.backend.alerts.AlertRepository;
import com.aegismonitor.backend.alerts.AlertRule;
import com.aegismonitor.backend.alerts.AlertService;
import com.aegismonitor.backend.alerts.JdbcAlertRepository;
import com.aegismonitor.backend.demo.DemoDataSeeder;
import com.aegismonitor.backend.metrics.HostMetricIngestionService;
import com.aegismonitor.backend.metrics.HostMetricRepository;
import com.aegismonitor.backend.metrics.JdbcHostMetricRepository;
import com.aegismonitor.backend.services.JdbcServiceRepository;
import com.aegismonitor.backend.services.ServiceInventory;
import com.aegismonitor.backend.services.ServiceRepository;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BackendConfiguration {
    @Bean
    AgentRegistry agentRegistry(
        @Value("${aegis.agent.register-token:demo-register-token}") String registerToken,
        AgentRepository agentRepository
    ) {
        return new AgentRegistry(registerToken, agentRepository);
    }

    @Bean
    AgentRepository agentRepository(DataSource dataSource) {
        return new JdbcAgentRepository(dataSource);
    }

    @Bean
    DemoDataSeeder demoDataSeeder(
        AgentRepository agentRepository,
        ServiceInventory serviceInventory,
        AlertRepository alertRepository,
        HostMetricIngestionService hostMetricIngestionService
    ) {
        return new DemoDataSeeder(
            agentRepository,
            serviceInventory,
            alertRepository,
            hostMetricIngestionService
        );
    }

    @Bean
    HostMetricIngestionService hostMetricIngestionService(HostMetricRepository hostMetricRepository) {
        return new HostMetricIngestionService(hostMetricRepository);
    }

    @Bean
    HostMetricRepository hostMetricRepository(DataSource dataSource) {
        return new JdbcHostMetricRepository(dataSource);
    }

    @Bean
    AlertService alertService(
        @Value("${aegis.alert.cpu-high-threshold:80.0}") double cpuHighThreshold,
        @Value("${aegis.alert.memory-high-threshold:85.0}") double memoryHighThreshold,
        @Value("${aegis.alert.tcp-connection-high-threshold:1000.0}") double tcpConnectionHighThreshold,
        AlertRepository alertRepository
    ) {
        AlertService alertService = new AlertService(alertRepository);
        alertService.addRule(
            new AlertRule("rule_cpu_high", "CPU_HIGH", cpuHighThreshold, "CRITICAL")
        );
        alertService.addRule(
            new AlertRule("rule_memory_high", "MEMORY_HIGH", memoryHighThreshold, "WARNING")
        );
        alertService.addRule(
            new AlertRule("rule_tcp_connection_high", "TCP_CONNECTION_HIGH", tcpConnectionHighThreshold, "WARNING")
        );
        return alertService;
    }

    @Bean
    AlertRepository alertRepository(DataSource dataSource) {
        return new JdbcAlertRepository(dataSource);
    }

    @Bean
    ServiceInventory serviceInventory(ServiceRepository serviceRepository) {
        return new ServiceInventory(serviceRepository);
    }

    @Bean
    ServiceRepository serviceRepository(DataSource dataSource) {
        return new JdbcServiceRepository(dataSource);
    }
}
