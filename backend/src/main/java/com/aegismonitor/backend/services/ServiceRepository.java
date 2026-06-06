package com.aegismonitor.backend.services;

import java.util.List;
import java.util.Optional;

public interface ServiceRepository {
    void upsert(ServiceInstance service);

    List<ServiceInstance> findByHostId(String hostId);

    Optional<ServiceInstance> findByKey(String hostId, String stackType, String serviceName);
}
