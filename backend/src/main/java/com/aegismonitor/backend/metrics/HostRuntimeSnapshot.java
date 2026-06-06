package com.aegismonitor.backend.metrics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class HostRuntimeSnapshot {
    private final String hostId;
    private final List<Integer> listeningPorts;
    private final String updatedAt;

    public HostRuntimeSnapshot(String hostId, List<Integer> listeningPorts, String updatedAt) {
        this.hostId = hostId;
        this.listeningPorts = Collections.unmodifiableList(new ArrayList<>(listeningPorts));
        this.updatedAt = updatedAt;
    }

    public String hostId() {
        return hostId;
    }

    public List<Integer> listeningPorts() {
        return listeningPorts;
    }

    public String updatedAt() {
        return updatedAt;
    }
}

