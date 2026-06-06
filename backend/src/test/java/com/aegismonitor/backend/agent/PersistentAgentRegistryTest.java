package com.aegismonitor.backend.agent;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

class PersistentAgentRegistryTest {
    private JdbcAgentRepository repository;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
            "jdbc:h2:mem:persistent_agent_registry;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
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
    void keepsRegisteredAgentAvailableAfterRegistryRecreation() {
        AgentRegistry firstRegistry = new AgentRegistry("demo-register-token", repository);

        AgentRegistrationResult identity = firstRegistry.register(
            "demo-register-token",
            new AgentRegistrationRequest(
                "DESKTOP-QIXING",
                "demo-host-a",
                "192.168.1.10",
                "Windows 11",
                "10.0.22631",
                8,
                17179869184L,
                "2026-06-04T09:00:00+08:00",
                "0.1.0"
            )
        );
        firstRegistry.heartbeat(
            new AgentHeartbeatRequest(
                identity.agentId(),
                identity.hostId(),
                identity.agentSecret(),
                "ONLINE",
                "2026-06-04T17:30:00+08:00"
            )
        );

        AgentRegistry recreatedRegistry = new AgentRegistry("demo-register-token", repository);

        List<AgentSummary> agents = recreatedRegistry.listAgents();

        assertThat(agents).hasSize(1);
        assertThat(agents.get(0).agentId()).isEqualTo("agt_001");
        assertThat(agents.get(0).hostname()).isEqualTo("DESKTOP-QIXING");
        assertThat(agents.get(0).status()).isEqualTo("ONLINE");
        assertThat(agents.get(0).lastHeartbeatAt()).isEqualTo("2026-06-04T17:30:00+08:00");
    }
}
