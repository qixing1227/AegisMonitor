package com.aegismonitor.backend.agent;

public final class AgentRegistryContractTest {
    public static void main(String[] args) {
        registersAgentAndAcceptsHeartbeat();
        rejectsInvalidRegisterToken();
    }

    private static void registersAgentAndAcceptsHeartbeat() {
        AgentRegistry registry = new AgentRegistry("demo-register-token");

        AgentRegistrationResult result = registry.register(
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

        assertEquals("host_001", result.hostId(), "host id");
        assertEquals("agt_001", result.agentId(), "agent id");
        assertTrue(result.agentSecret().length() > 10, "agent secret should be generated");

        registry.heartbeat(
            new AgentHeartbeatRequest(
                result.agentId(),
                result.hostId(),
                result.agentSecret(),
                "ONLINE",
                "2026-06-04T17:30:00+08:00"
            )
        );

        AgentStatus status = registry.getAgentStatus(result.agentId());

        assertEquals("ONLINE", status.status(), "agent status");
        assertEquals("2026-06-04T17:30:00+08:00", status.lastHeartbeatAt(), "last heartbeat");
    }

    private static void rejectsInvalidRegisterToken() {
        AgentRegistry registry = new AgentRegistry("demo-register-token");

        assertThrows(
            IllegalArgumentException.class,
            () -> registry.register(
                "wrong-token",
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
            )
        );
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void assertTrue(boolean condition, String label) {
        if (!condition) {
            throw new AssertionError(label);
        }
    }

    private static void assertThrows(Class<? extends Throwable> expected, ThrowingRunnable runnable) {
        try {
            runnable.run();
        } catch (Throwable actual) {
            if (expected.isInstance(actual)) {
                return;
            }
            throw new AssertionError("expected " + expected.getName() + " but caught " + actual.getClass().getName(), actual);
        }
        throw new AssertionError("expected " + expected.getName() + " but nothing was thrown");
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run();
    }
}
