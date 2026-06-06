package com.aegismonitor.backend.services;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class InMemoryServiceRepository implements ServiceRepository {
    private final Map<String, ServiceInstance> latestByServiceKey = new HashMap<>();

    @Override
    public void upsert(ServiceInstance service) {
        latestByServiceKey.put(
            key(service.hostId(), service.stackType(), service.serviceName()),
            service
        );
    }

    @Override
    public List<ServiceInstance> findByHostId(String hostId) {
        List<ServiceInstance> services = new ArrayList<>();
        for (ServiceInstance service : latestByServiceKey.values()) {
            if (service.hostId().equals(hostId)) {
                services.add(service);
            }
        }
        services.sort(
            Comparator.comparing(ServiceInstance::stackType)
                .thenComparing(ServiceInstance::serviceName)
        );
        return services;
    }

    @Override
    public Optional<ServiceInstance> findByKey(String hostId, String stackType, String serviceName) {
        return Optional.ofNullable(latestByServiceKey.get(key(hostId, stackType, serviceName)));
    }

    private static String key(String hostId, String stackType, String serviceName) {
        return hostId + "|" + stackType + "|" + serviceName;
    }
}
