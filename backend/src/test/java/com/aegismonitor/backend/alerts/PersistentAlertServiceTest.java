package com.aegismonitor.backend.alerts;

import static org.assertj.core.api.Assertions.assertThat;

import com.aegismonitor.backend.metrics.HostMetricPoint;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

class PersistentAlertServiceTest {
    private JdbcAlertRepository repository;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
            "jdbc:h2:mem:persistent_alert_service;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
            "sa",
            ""
        );
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
            new ClassPathResource("schema.sql")
        );
        populator.execute(dataSource);
        repository = new JdbcAlertRepository(dataSource);
    }

    @Test
    void keepsAcknowledgedAlertAvailableAfterServiceRecreation() {
        AlertService firstService = new AlertService(repository);
        firstService.addRule(new AlertRule("rule_cpu_high", "CPU_HIGH", 80.0, "CRITICAL"));

        firstService.evaluate(
            new HostMetricPoint(
                "host_001",
                "2026-06-04T17:31:00+08:00",
                91.5,
                50.0,
                128
            )
        );
        firstService.acknowledge(
            "alert_001",
            "ops_001",
            "2026-06-04T17:32:00+08:00",
            "已通知现场同学排查 CPU 占用进程"
        );

        AlertService recreatedService = new AlertService(repository);

        List<AlertEvent> events = recreatedService.listEvents();

        assertThat(events).hasSize(1);
        assertThat(events.get(0).eventId()).isEqualTo("alert_001");
        assertThat(events.get(0).status()).isEqualTo("ACKED");
        assertThat(events.get(0).acknowledgedBy()).isEqualTo("ops_001");
        assertThat(events.get(0).acknowledgedAt()).isEqualTo("2026-06-04T17:32:00+08:00");
        assertThat(events.get(0).ackNote()).isEqualTo("已通知现场同学排查 CPU 占用进程");
    }
}
