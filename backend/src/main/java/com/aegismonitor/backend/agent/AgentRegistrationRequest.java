package com.aegismonitor.backend.agent;

public final class AgentRegistrationRequest {
    private final String hostname;
    private final String alias;
    private final String ipAddress;
    private final String osName;
    private final String osVersion;
    private final int cpuCores;
    private final long memoryTotalBytes;
    private final String bootTime;
    private final String agentVersion;

    public AgentRegistrationRequest(
        String hostname,
        String alias,
        String ipAddress,
        String osName,
        String osVersion,
        int cpuCores,
        long memoryTotalBytes,
        String bootTime,
        String agentVersion
    ) {
        this.hostname = hostname;
        this.alias = alias;
        this.ipAddress = ipAddress;
        this.osName = osName;
        this.osVersion = osVersion;
        this.cpuCores = cpuCores;
        this.memoryTotalBytes = memoryTotalBytes;
        this.bootTime = bootTime;
        this.agentVersion = agentVersion;
    }

    public String hostname() {
        return hostname;
    }

    public String alias() {
        return alias;
    }

    public String ipAddress() {
        return ipAddress;
    }

    public String osName() {
        return osName;
    }

    public String osVersion() {
        return osVersion;
    }

    public int cpuCores() {
        return cpuCores;
    }

    public long memoryTotalBytes() {
        return memoryTotalBytes;
    }

    public String bootTime() {
        return bootTime;
    }

    public String agentVersion() {
        return agentVersion;
    }
}

