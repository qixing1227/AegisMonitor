package com.aegismonitor.backend.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ServiceInstance {
    private final String hostId;
    private final String serviceName;
    private final String stackType;
    private final String processName;
    private final int pid;
    private final List<Integer> ports;
    private final String status;
    private final String commandLine;
    private final String lastSeenAt;

    public ServiceInstance(
        String hostId,
        String serviceName,
        String stackType,
        String processName,
        int pid,
        List<Integer> ports,
        String status,
        String commandLine,
        String lastSeenAt
    ) {
        this.hostId = hostId;
        this.serviceName = serviceName;
        this.stackType = stackType;
        this.processName = processName;
        this.pid = pid;
        this.ports = Collections.unmodifiableList(new ArrayList<>(ports));
        this.status = status;
        this.commandLine = commandLine;
        this.lastSeenAt = lastSeenAt;
    }

    public String hostId() {
        return hostId;
    }

    public String serviceName() {
        return serviceName;
    }

    public String stackType() {
        return stackType;
    }

    public String processName() {
        return processName;
    }

    public int pid() {
        return pid;
    }

    public List<Integer> ports() {
        return ports;
    }

    public String status() {
        return status;
    }

    public String commandLine() {
        return commandLine;
    }

    public String lastSeenAt() {
        return lastSeenAt;
    }
}
