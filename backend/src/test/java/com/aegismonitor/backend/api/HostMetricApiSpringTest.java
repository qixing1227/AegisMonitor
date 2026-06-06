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
class HostMetricApiSpringTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void acceptsHostMetricsFromRegisteredAgentThroughHttpContract() throws Exception {
        JsonNode identity = registerAgent();

        mockMvc.perform(
                post("/api/metrics/host")
                    .header("X-Agent-Id", identity.path("agentId").asText())
                    .header("X-Agent-Secret", identity.path("agentSecret").asText())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "agentId": "agt_001",
                          "hostId": "host_001",
                          "reportedAt": "2026-06-04T17:30:00+08:00",
                          "cpu": {
                            "usagePercent": 42.5,
                            "perCoreUsagePercent": [35.1, 47.2]
                          },
                          "memory": {
                            "totalBytes": 17179869184,
                            "usedBytes": 8589934592,
                            "availableBytes": 8589934592,
                            "usagePercent": 50.0
                          },
                          "disks": [
                            {
                              "mountPoint": "C:",
                              "totalBytes": 536870912000,
                              "usedBytes": 322122547200,
                              "freeBytes": 214748364800,
                              "usagePercent": 60.0
                            }
                          ],
                          "networks": [
                            {
                              "interfaceName": "WLAN",
                              "bytesSent": 123456789,
                              "bytesReceived": 987654321,
                              "sendRateBytesPerSecond": 2048,
                              "receiveRateBytesPerSecond": 4096
                            }
                          ],
                          "tcp": {
                            "connectionCount": 128,
                            "listeningPorts": [80, 3306, 6379, 8080]
                          }
                        }
                        """)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("OK"))
            .andExpect(jsonPath("$.message").value("metrics accepted"))
            .andExpect(jsonPath("$.data.written").value(true))
            .andExpect(jsonPath("$.data.generatedAlertCount").value(0));
    }

    @Test
    void returnsGeneratedAlertCountWhenHostMetricBreaksCpuThreshold() throws Exception {
        JsonNode identity = registerAgent();

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
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("metrics accepted"))
            .andExpect(jsonPath("$.data.written").value(true))
            .andExpect(jsonPath("$.data.generatedAlertCount").value(1));
    }

    @Test
    void returnsLatestHostMetricSnapshotForOperationsDashboard() throws Exception {
        JsonNode identity = registerAgent();

        reportHostMetrics(identity, "2026-06-04T17:30:00+08:00", 42.5, 50.0, 128);
        reportHostMetrics(identity, "2026-06-04T17:31:00+08:00", 67.2, 58.5, 144);

        mockMvc.perform(
                get("/api/metrics/host/latest")
                    .queryParam("hostId", "host_001")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("latest host metrics"))
            .andExpect(jsonPath("$.data.hostId").value("host_001"))
            .andExpect(jsonPath("$.data.reportedAt").value("2026-06-04T17:31:00+08:00"))
            .andExpect(jsonPath("$.data.cpuUsagePercent").value(67.2))
            .andExpect(jsonPath("$.data.memoryUsagePercent").value(58.5))
            .andExpect(jsonPath("$.data.tcpConnectionCount").value(144));
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

    private void reportHostMetrics(
        JsonNode identity,
        String reportedAt,
        double cpuUsagePercent,
        double memoryUsagePercent,
        int tcpConnectionCount
    ) throws Exception {
        mockMvc.perform(
                post("/api/metrics/host")
                    .header("X-Agent-Id", identity.path("agentId").asText())
                    .header("X-Agent-Secret", identity.path("agentSecret").asText())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "agentId": "agt_001",
                          "hostId": "host_001",
                          "reportedAt": "%s",
                          "cpu": {
                            "usagePercent": %.1f,
                            "perCoreUsagePercent": [35.1, 47.2]
                          },
                          "memory": {
                            "totalBytes": 17179869184,
                            "usedBytes": 8589934592,
                            "availableBytes": 8589934592,
                            "usagePercent": %.1f
                          },
                          "disks": [],
                          "networks": [],
                          "tcp": {
                            "connectionCount": %d,
                            "listeningPorts": [80, 3306, 6379, 8080]
                          }
                        }
                        """.formatted(
                            reportedAt,
                            cpuUsagePercent,
                            memoryUsagePercent,
                            tcpConnectionCount
                        ))
            )
            .andExpect(status().isOk());
    }
}
