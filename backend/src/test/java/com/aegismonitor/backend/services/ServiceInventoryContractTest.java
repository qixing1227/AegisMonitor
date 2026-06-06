package com.aegismonitor.backend.services;

import java.util.Arrays;
import java.util.List;

public final class ServiceInventoryContractTest {
    public static void main(String[] args) {
        upsertsLatestServicesPerHost();
    }

    private static void upsertsLatestServicesPerHost() {
        ServiceInventory inventory = new ServiceInventory();

        int firstCount = inventory.report(
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

        int secondCount = inventory.report(
            new ServiceDiscoveryReport(
                "agt_001",
                "host_001",
                "2026-06-04T17:36:00+08:00",
                Arrays.asList(
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

        List<ServiceInstance> services = inventory.latestServices("host_001");
        ServiceInstance springBoot = inventory.get("host_001", "SPRING_BOOT", "aegis-backend");

        assertEquals(2, firstCount, "first upsert count");
        assertEquals(1, secondCount, "second upsert count");
        assertEquals(2, services.size(), "service inventory size");
        assertEquals("host_001", springBoot.hostId(), "service host id");
        assertEquals("aegis-backend", springBoot.serviceName(), "service name");
        assertEquals("SPRING_BOOT", springBoot.stackType(), "stack type");
        assertEquals(20480, springBoot.pid(), "updated pid");
        assertEquals(Arrays.asList(18080), springBoot.ports(), "updated ports");
        assertEquals("2026-06-04T17:36:00+08:00", springBoot.lastSeenAt(), "last seen");
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected <" + expected + "> but was <" + actual + ">");
        }
    }
}
