package com.aegismonitor.backend.agent;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

class JdbcAgentRepositoryTest {
    private JdbcAgentRepository repository;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
            "jdbc:h2:mem:aegis_agent_repository;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
            "sa",
            ""
        );
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
            new ClassPathResource("schema.sql")
        );
        populator.execute(dataSource);
        repository = new JdbcAgentRepository(dataSource);
    }

    @Test
    void persistsRegisteredAgentAndUpdatesHeartbeatStatus() {
        repository.save(
            new AgentRecord(
                "agt_001",
                "host_001",
                "generated-agent-secret",
                "DESKTOP-QIXING",
                "demo-host-a",
                "192.168.1.10",
                "Windows 11",
                "10.0.22631",
                8,
                17179869184L,
                "2026-06-04T09:00:00+08:00",
                "0.1.0",
                "ONLINE",
                null,
                "2026-06-04T17:29:00+08:00"
            )
        );

        Optional<AgentRecord> saved = repository.findByAgentId("agt_001");

        assertThat(saved).isPresent();
        assertThat(saved.get().hostId()).isEqualTo("host_001");
        assertThat(saved.get().hostname()).isEqualTo("DESKTOP-QIXING");
        assertThat(saved.get().agentSecret()).isEqualTo("generated-agent-secret");
        assertThat(saved.get().status()).isEqualTo("ONLINE");

        repository.updateHeartbeat(
            "agt_001",
            "OFFLINE",
            "2026-06-04T17:40:00+08:00"
        );

        AgentRecord updated = repository.findByAgentId("agt_001").orElseThrow();

        assertThat(updated.status()).isEqualTo("OFFLINE");
        assertThat(updated.lastHeartbeatAt()).isEqualTo("2026-06-04T17:40:00+08:00");
    }
}
