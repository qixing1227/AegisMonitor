package com.aegismonitor.backend.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DiscoveredServiceReport {
    private final String serviceName;
    private final String stackType;
    private final String processName;
    private final int pid;
    private final List<Integer> ports;
    private final String status;
    private final String commandLine;
    private final double processCpuPercent;
    private final long processMemoryBytes;

    public DiscoveredServiceReport(
        String serviceName,
        String stackType,
        String processName,
        int pid,
        List<Integer> ports,
        String status,
        String commandLine
    ) {
        this(serviceName, stackType, processName, pid, ports, status, commandLine, 0.0, 0L);
    }

    public DiscoveredServiceReport(
        String serviceName,
        String stackType,
        String processName,
        int pid,
        List<Integer> ports,
        String status,
        String commandLine,
        double processCpuPercent,
        long processMemoryBytes
    ) {
        this.serviceName = serviceName;
        this.stackType = stackType;
        this.processName = processName;
        this.pid = pid;
        this.ports = Collections.unmodifiableList(new ArrayList<>(ports));
        this.status = status;
        this.commandLine = commandLine;
        this.processCpuPercent = processCpuPercent;
        this.processMemoryBytes = processMemoryBytes;
    }

    public String serviceName() { return serviceName; }

    public String stackType() { return stackType; }

    public String processName() { return processName; }

    public int pid() { return pid; }

    public List<Integer> ports() { return ports; }

    public String status() { return status; }

    public String commandLine() { return commandLine; }

    public double processCpuPercent() { return processCpuPercent; }

    public long processMemoryBytes() { return processMemoryBytes; }
}
