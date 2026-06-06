package com.aegismonitor.backend.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class DemoSeedApiSpringTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void seedsDemoHostsIdempotentlyForFrontendDashboard() throws Exception {
        seedDemoData();
        seedDemoData();

        mockMvc.perform(get("/api/agents"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(3))
            .andExpect(jsonPath("$.data[0].agentId").value("demo_agt_001"))
            .andExpect(jsonPath("$.data[0].hostId").value("demo_host_001"))
            .andExpect(jsonPath("$.data[0].hostname").value("demo-web-01"))
            .andExpect(jsonPath("$.data[0].alias").value("模拟 Web 主机"))
            .andExpect(jsonPath("$.data[0].ipAddress").value("10.0.0.11"))
            .andExpect(jsonPath("$.data[0].status").value("ONLINE"))
            .andExpect(jsonPath("$.data[0].agentSecret").doesNotExist());
    }

    @Test
    void seedsDemoServicesForFrontendServicePage() throws Exception {
        seedDemoData();

        mockMvc.perform(get("/api/services").queryParam("hostId", "demo_host_001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].hostId").value("demo_host_001"))
            .andExpect(jsonPath("$.data[0].serviceName").value("nginx"))
            .andExpect(jsonPath("$.data[0].stackType").value("NGINX"))
            .andExpect(jsonPath("$.data[0].ports[0]").value(80))
            .andExpect(jsonPath("$.data[0].status").value("RUNNING"));
    }

    @Test
    void seedsDemoAlertsForFrontendAckFlow() throws Exception {
        seedDemoData();
        seedDemoData();

        mockMvc.perform(get("/api/alerts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].hostId").value("demo_host_003"))
            .andExpect(jsonPath("$.data[0].metricName").value("CPU_HIGH"))
            .andExpect(jsonPath("$.data[0].severity").value("CRITICAL"))
            .andExpect(jsonPath("$.data[0].status").value("OPEN"))
            .andExpect(jsonPath("$.data[0].actualValue").value(93.5));
    }

    private void seedDemoData() throws Exception {
        mockMvc.perform(
                post("/api/demo/seed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "hostCount": 3,
                          "includeServices": true,
                          "includeAlerts": true
                        }
                        """)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("demo data seeded"))
            .andExpect(jsonPath("$.data.hostsCreated").value(3));
    }
}
