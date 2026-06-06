package com.aegismonitor.backend.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AlertApiSpringTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void listsOpenAlertsGeneratedByHostMetricsForOperationsDashboard() throws Exception {
        JsonNode identity = registerAgent();
        reportHighCpuMetric(identity);

        mockMvc.perform(get("/api/alerts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("OK"))
            .andExpect(jsonPath("$.message").value("alert list"))
            .andExpect(jsonPath("$.data[0].eventId").value("alert_001"))
            .andExpect(jsonPath("$.data[0].ruleId").value("rule_cpu_high"))
            .andExpect(jsonPath("$.data[0].hostId").value("host_001"))
            .andExpect(jsonPath("$.data[0].metricName").value("CPU_HIGH"))
            .andExpect(jsonPath("$.data[0].severity").value("CRITICAL"))
            .andExpect(jsonPath("$.data[0].threshold").value(80.0))
            .andExpect(jsonPath("$.data[0].actualValue").value(91.5))
            .andExpect(jsonPath("$.data[0].status").value("OPEN"))
            .andExpect(jsonPath("$.data[0].occurredAt").value("2026-06-04T17:31:00+08:00"));
    }

    @Test
    void acknowledgesAlertThroughHttpContract() throws Exception {
        JsonNode identity = registerAgent();
        reportHighCpuMetric(identity);

        mockMvc.perform(
                post("/api/alerts/alert_001/ack")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "acknowledgedBy": "ops_001",
                          "acknowledgedAt": "2026-06-04T17:32:00+08:00",
                          "ackNote": "已通知现场同学排查 CPU 占用进程"
                        }
                        """)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("OK"))
            .andExpect(jsonPath("$.message").value("alert acknowledged"))
            .andExpect(jsonPath("$.data.eventId").value("alert_001"))
            .andExpect(jsonPath("$.data.status").value("ACKED"))
            .andExpect(jsonPath("$.data.acknowledgedBy").value("ops_001"))
            .andExpect(jsonPath("$.data.acknowledgedAt").value("2026-06-04T17:32:00+08:00"))
            .andExpect(jsonPath("$.data.ackNote").value("已通知现场同学排查 CPU 占用进程"));

        mockMvc.perform(get("/api/alerts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].status").value("ACKED"));
    }

    private JsonNode registerAgent() throws Exception {
        MvcResult registration = mockMvc.perform(
                post("/api/agents/register")
                    .header("X-Agent-Register-Token", "demo-register-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "hostname": "DESKTOP-QIXING",
                          "alias": "demo-host-a",
                          "ipAddress": "192.168.1.10",
                          "osName": "Windows 11",
                          "osVersion": "10.0.22631",
                          "cpuCores": 8,
                          "memoryTotalBytes": 17179869184,
                          "bootTime": "2026-06-04T09:00:00+08:00",
                          "agentVersion": "0.1.0",
                          "configSummary": {
                            "hostMetricIntervalSeconds": 5,
                            "heartbeatIntervalSeconds": 10,
                            "serviceDiscoveryIntervalSeconds": 30
                          }
                        }
                        """)
            )
            .andExpect(status().isOk())
            .andReturn();

        return objectMapper
            .readTree(registration.getResponse().getContentAsString())
            .path("data");
    }

    private void reportHighCpuMetric(JsonNode identity) throws Exception {
        mockMvc.perform(
                post("/api/metrics/host")
                    .header("X-Agent-Id", identity.path("agentId").asText())
                    .header("X-Agent-Secret", identity.path("agentSecret").asText())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "agentId": "agt_001",
                          "hostId": "host_001",
                          "reportedAt": "2026-06-04T17:31:00+08:00",
                          "cpu": {
                            "usagePercent": 91.5,
                            "perCoreUsagePercent": [95.0, 88.0]
                          },
                          "memory": {
                            "totalBytes": 17179869184,
                            "usedBytes": 8589934592,
                            "availableBytes": 8589934592,
                            "usagePercent": 50.0
                          },
                          "disks": [],
                          "networks": [],
                          "tcp": {
                            "connectionCount": 128,
                            "listeningPorts": [80, 3306, 6379, 8080]
                          }
                        }
                        """)
            )
            .andExpect(status().isOk());
    }
}
