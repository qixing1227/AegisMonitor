package com.aegismonitor.backend.alerts;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class InMemoryAlertRepository implements AlertRepository {
    private final Map<String, AlertEvent> eventsById = new LinkedHashMap<>();

    @Override
    public void save(AlertEvent event) {
        eventsById.put(event.eventId(), event);
    }

    @Override
    public Optional<AlertEvent> findByEventId(String eventId) {
        return Optional.ofNullable(eventsById.get(eventId));
    }

    @Override
    public Optional<AlertEvent> findOpenByHostIdAndRuleId(String hostId, String ruleId) {
        return eventsById.values()
            .stream()
            .filter(event -> hostId.equals(event.hostId()))
            .filter(event -> ruleId.equals(event.ruleId()))
            .filter(event -> "OPEN".equals(event.status()))
            .findFirst();
    }

    @Override
    public void update(AlertEvent event) {
        eventsById.put(event.eventId(), event);
    }

    @Override
    public List<AlertEvent> findAll() {
        return new ArrayList<>(eventsById.values());
    }
}
