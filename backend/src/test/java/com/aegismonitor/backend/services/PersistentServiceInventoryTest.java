package com.aegismonitor.backend.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

class PersistentServiceInventoryTest {
    private JdbcServiceRepository repository;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
            "jdbc:h2:mem:persistent_service_inventory;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
            "sa",
            ""
        );
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
            new ClassPathResource("schema.sql")
        );
        populator.execute(dataSource);
        repository = new JdbcServiceRepository(dataSource);
    }

    @Test
    void keepsLatestServicesAvailableAfterInventoryRecreation() {
        ServiceInventory firstInventory = new ServiceInventory(repository);

        firstInventory.report(
            new ServiceDiscoveryReport(
                "agt_001",
                "host_001",
                "2026-06-04T17:35:00+08:00",
                Arrays.asList(
                    new DiscoveredServiceReport(
                        "aegis-backend",
                        "SPRING_BOOT",
                        "java.exe",
                        10240,
                        Arrays.asList(8080),
                        "RUNNING",
                        "java -jar aegis-backend.jar"
                    ),
                    new DiscoveredServiceReport(
                        "mysql",
                        "MYSQL",
                        "mysqld.exe",
                        3306,
                        Arrays.asList(3306),
                        "RUNNING",
                        "mysqld.exe"
                    )
                )
            )
        );
        firstInventory.report(
            new ServiceDiscoveryReport(
                "agt_001",
                "host_001",
                "2026-06-04T17:36:00+08:00",
                List.of(
                    new DiscoveredServiceReport(
                        "aegis-backend",
                        "SPRING_BOOT",
                        "java.exe",
                        20480,
                        Arrays.asList(18080),
                        "RUNNING",
                        "java -jar aegis-backend.jar --server.port=18080"
                    )
                )
            )
        );

        ServiceInventory recreatedInventory = new ServiceInventory(repository);

        List<ServiceInstance> services = recreatedInventory.latestServices("host_001");

        assertThat(services).hasSize(2);
        assertThat(services.get(0).serviceName()).isEqualTo("mysql");
        assertThat(services.get(0).ports()).containsExactly(3306);
        assertThat(services.get(1).serviceName()).isEqualTo("aegis-backend");
        assertThat(services.get(1).pid()).isEqualTo(20480);
        assertThat(services.get(1).ports()).containsExactly(18080);
        assertThat(services.get(1).lastSeenAt()).isEqualTo("2026-06-04T17:36:00+08:00");
    }
}
