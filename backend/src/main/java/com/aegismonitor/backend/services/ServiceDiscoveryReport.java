package com.aegismonitor.backend.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ServiceDiscoveryReport {
    private final String agentId;
    private final String hostId;
    private final String reportedAt;
    private final List<DiscoveredServiceReport> services;

    public ServiceDiscoveryReport(
        String agentId,
        String hostId,
        String reportedAt,
        List<DiscoveredServiceReport> services
    ) {
        this.agentId = agentId;
        this.hostId = hostId;
        this.reportedAt = reportedAt;
        this.services = Collections.unmodifiableList(new ArrayList<>(services));
    }

    public String agentId() {
        return agentId;
    }

    public String hostId() {
        return hostId;
    }

    public String reportedAt() {
        return reportedAt;
    }

    public List<DiscoveredServiceReport> services() {
        return services;
    }
}
