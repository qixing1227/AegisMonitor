package com.aegismonitor.backend.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class AgentApiSpringTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registersAgentThroughHttpContract() throws Exception {
        mockMvc.perform(
                post("/api/agents/register")
                    .header("X-Agent-Register-Token", "demo-register-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(registrationRequestJson())
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("OK"))
            .andExpect(jsonPath("$.message").value("registered"))
            .andExpect(jsonPath("$.data.agentId").value("agt_001"))
            .andExpect(jsonPath("$.data.hostId").value("host_001"))
            .andExpect(jsonPath("$.data.agentSecret").isString())
            .andExpect(jsonPath("$.data.approved").value(true));
    }

    @Test
    void acceptsHeartbeatFromRegisteredAgentThroughHttpContract() throws Exception {
        MvcResult registration = mockMvc.perform(
                post("/api/agents/register")
                    .header("X-Agent-Register-Token", "demo-register-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(registrationRequestJson())
            )
            .andExpect(status().isOk())
            .andReturn();

        JsonNode identity = objectMapper
            .readTree(registration.getResponse().getContentAsString())
            .path("data");

        mockMvc.perform(
                post("/api/agents/heartbeat")
                    .header("X-Agent-Id", identity.path("agentId").asText())
                    .header("X-Agent-Secret", identity.path("agentSecret").asText())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "agentId": "agt_001",
                          "hostId": "host_001",
                          "status": "ONLINE",
                          "reportedAt": "2026-06-04T17:30:00+08:00"
                        }
                        """)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("OK"))
            .andExpect(jsonPath("$.message").value("heartbeat accepted"))
            .andExpect(jsonPath("$.data.serverTime").isString());
    }

    @Test
    void listsRegisteredAgentsForOperationsDashboard() throws Exception {
        MvcResult registration = mockMvc.perform(
                post("/api/agents/register")
                    .header("X-Agent-Register-Token", "demo-register-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(registrationRequestJson())
            )
            .andExpect(status().isOk())
            .andReturn();

        JsonNode identity = objectMapper
            .readTree(registration.getResponse().getContentAsString())
            .path("data");

        mockMvc.perform(
                post("/api/agents/heartbeat")
                    .header("X-Agent-Id", identity.path("agentId").asText())
                    .header("X-Agent-Secret", identity.path("agentSecret").asText())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "agentId": "agt_001",
                          "hostId": "host_001",
                          "status": "ONLINE",
                          "reportedAt": "2026-06-04T17:30:00+08:00"
                        }
                        """)
            )
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/agents"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("OK"))
            .andExpect(jsonPath("$.message").value("agent list"))
            .andExpect(jsonPath("$.data[0].agentId").value("agt_001"))
            .andExpect(jsonPath("$.data[0].hostId").value("host_001"))
            .andExpect(jsonPath("$.data[0].hostname").value("DESKTOP-QIXING"))
            .andExpect(jsonPath("$.data[0].alias").value("demo-host-a"))
            .andExpect(jsonPath("$.data[0].ipAddress").value("192.168.1.10"))
            .andExpect(jsonPath("$.data[0].status").value("ONLINE"))
            .andExpect(jsonPath("$.data[0].lastHeartbeatAt").value("2026-06-04T17:30:00+08:00"))
            .andExpect(jsonPath("$.data[0].agentSecret").doesNotExist());
    }

    private static String registrationRequestJson() {
        return """
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
            """;
    }
}
