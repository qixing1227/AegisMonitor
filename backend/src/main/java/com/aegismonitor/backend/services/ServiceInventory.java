package com.aegismonitor.backend.services;

import java.util.List;

public final class ServiceInventory {
    private final ServiceRepository repository;

    public ServiceInventory() {
        this(new InMemoryServiceRepository());
    }

    public ServiceInventory(ServiceRepository repository) {
        this.repository = repository;
    }

    public int report(ServiceDiscoveryReport report) {
        int upsertedCount = 0;
        for (DiscoveredServiceReport service : report.services()) {
            repository.upsert(
                new ServiceInstance(
                    report.hostId(),
                    service.serviceName(),
                    service.stackType(),
                    service.processName(),
                    service.pid(),
                    service.ports(),
                    service.status(),
                    service.commandLine(),
                    report.reportedAt()
                )
            );
            upsertedCount++;
        }
        return upsertedCount;
    }

    public List<ServiceInstance> latestServices(String hostId) {
        return repository.findByHostId(hostId);
    }

    public ServiceInstance get(String hostId, String stackType, String serviceName) {
        return repository.findByKey(hostId, stackType, serviceName)
            .orElseThrow(
                () -> new IllegalArgumentException(
                    "No service found for host " + hostId + " and service " + serviceName
                )
            );
    }
}
