package com.aegismonitor.backend.error;

public final class AgentAccessException extends RuntimeException {
    private final AccessType accessType;

    private AgentAccessException(AccessType accessType, String message) {
        super(message);
        this.accessType = accessType;
    }

    public static AgentAccessException unauthorized() {
        return new AgentAccessException(
            AccessType.UNAUTHORIZED,
            "Agent credentials are invalid"
        );
    }

    public static AgentAccessException forbidden() {
        return new AgentAccessException(
            AccessType.FORBIDDEN,
            "Agent is not authorized for this host"
        );
    }

    public AccessType accessType() {
        return accessType;
    }

    public enum AccessType {
        UNAUTHORIZED,
        FORBIDDEN
    }
}