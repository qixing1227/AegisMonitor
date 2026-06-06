package com.aegismonitor.backend.alerts;

import java.util.List;
import java.util.Optional;

public interface AlertRepository {
    void save(AlertEvent event);

    Optional<AlertEvent> findByEventId(String eventId);

    Optional<AlertEvent> findOpenByHostIdAndRuleId(String hostId, String ruleId);

    void update(AlertEvent event);

    List<AlertEvent> findAll();
}
