package com.aegismonitor.backend.metrics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TcpSample {
    private final int connectionCount;
    private final List<Integer> listeningPorts;

    public TcpSample(int connectionCount, List<Integer> listeningPorts) {
        this.connectionCount = connectionCount;
        this.listeningPorts = Collections.unmodifiableList(new ArrayList<>(listeningPorts));
    }

    public int connectionCount() {
        return connectionCount;
    }

    public List<Integer> listeningPorts() {
        return listeningPorts;
    }
}

