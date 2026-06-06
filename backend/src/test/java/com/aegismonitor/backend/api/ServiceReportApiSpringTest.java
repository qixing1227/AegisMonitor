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
class ServiceReportApiSpringTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void acceptsServiceDiscoveryReportFromRegisteredAgentThroughHttpContract() throws Exception {
        JsonNode identity = registerAgent();

        mockMvc.perform(
                post("/api/services/report")
                    .header("X-Agent-Id", identity.path("agentId").asText())
                    .header("X-Agent-Secret", identity.path("agentSecret").asText())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "agentId": "agt_001",
                          "hostId": "host_001",
                          "reportedAt": "2026-06-04T17:35:00+08:00",
                          "services": [
                            {
                              "serviceName": "aegis-backend",
                              "stackType": "SPRING_BOOT",
                              "processName": "java.exe",
                              "pid": 10240,
                              "ports": [8080],
                              "status": "RUNNING",
                              "commandLine": "java -jar aegis-backend.jar"
                            },
                            {
                              "serviceName": "mysql",
                              "stackType": "MYSQL",
                              "processName": "mysqld.exe",
                              "pid": 3306,
                              "ports": [3306],
                              "status": "RUNNING",
                              "commandLine": "mysqld.exe"
                            }
                          ]
                        }
                        """)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("OK"))
            .andExpect(jsonPath("$.message").value("services accepted"))
            .andExpect(jsonPath("$.data.upsertedCount").value(2));
    }

    @Test
    void listsLatestServicesForOperationsDashboard() throws Exception {
        JsonNode identity = registerAgent();
        reportServices(identity);

        mockMvc.perform(get("/api/services").param("hostId", "host_001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("OK"))
            .andExpect(jsonPath("$.message").value("service list"))
            .andExpect(jsonPath("$.data[0].hostId").value("host_001"))
            .andExpect(jsonPath("$.data[0].serviceName").value("mysql"))
            .andExpect(jsonPath("$.data[0].stackType").value("MYSQL"))
            .andExpect(jsonPath("$.data[0].processName").value("mysqld.exe"))
            .andExpect(jsonPath("$.data[0].pid").value(3306))
            .andExpect(jsonPath("$.data[0].ports[0]").value(3306))
            .andExpect(jsonPath("$.data[0].status").value("RUNNING"))
            .andExpect(jsonPath("$.data[0].lastSeenAt").value("2026-06-04T17:35:00+08:00"))
            .andExpect(jsonPath("$.data[1].serviceName").value("aegis-backend"))
            .andExpect(jsonPath("$.data[1].stackType").value("SPRING_BOOT"))
            .andExpect(jsonPath("$.data[1].ports[0]").value(8080));
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

    private void reportServices(JsonNode identity) throws Exception {
        mockMvc.perform(
                post("/api/services/report")
                    .header("X-Agent-Id", identity.path("agentId").asText())
                    .header("X-Agent-Secret", identity.path("agentSecret").asText())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "agentId": "agt_001",
                          "hostId": "host_001",
                          "reportedAt": "2026-06-04T17:35:00+08:00",
                          "services": [
                            {
                              "serviceName": "aegis-backend",
                              "stackType": "SPRING_BOOT",
                              "processName": "java.exe",
                              "pid": 10240,
                              "ports": [8080],
                              "status": "RUNNING",
                              "commandLine": "java -jar aegis-backend.jar"
                            },
                            {
                              "serviceName": "mysql",
                              "stackType": "MYSQL",
                              "processName": "mysqld.exe",
                              "pid": 3306,
                              "ports": [3306],
                              "status": "RUNNING",
                              "commandLine": "mysqld.exe"
                            }
                          ]
                        }
                        """)
            )
            .andExpect(status().isOk());
    }
}
