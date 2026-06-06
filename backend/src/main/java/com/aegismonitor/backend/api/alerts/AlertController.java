package com.aegismonitor.backend.api.alerts;

import com.aegismonitor.backend.alerts.AlertEvent;
import com.aegismonitor.backend.alerts.AlertService;
import com.aegismonitor.backend.api.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {
    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public ApiResponse<List<AlertEvent>> listAlerts() {
        return ApiResponse.ok("alert list", alertService.listEvents());
    }

    @PostMapping("/{eventId}/ack")
    public ApiResponse<AlertEvent> acknowledgeAlert(
        @PathVariable String eventId,
        @RequestBody AlertAckHttpRequest request
    ) {
        return ApiResponse.ok(
            "alert acknowledged",
            alertService.acknowledge(
                eventId,
                request.acknowledgedBy(),
                request.acknowledgedAt(),
                request.ackNote()
            )
        );
    }

    public record AlertAckHttpRequest(
        String acknowledgedBy,
        String acknowledgedAt,
        String ackNote
    ) {
    }
}
