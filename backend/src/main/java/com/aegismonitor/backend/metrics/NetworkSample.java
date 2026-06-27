package com.aegismonitor.backend.metrics;

public final class NetworkSample {
    private final long bytesSent;
    private final long bytesReceived;

    public NetworkSample(long bytesSent, long bytesReceived) {
        this.bytesSent = bytesSent;
        this.bytesReceived = bytesReceived;
    }

    public long bytesSent() {
        return bytesSent;
    }

    public long bytesReceived() {
        return bytesReceived;
    }
}
