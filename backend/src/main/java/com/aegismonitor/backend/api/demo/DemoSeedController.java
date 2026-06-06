package com.aegismonitor.backend.api.demo;

import com.aegismonitor.backend.api.ApiResponse;
import com.aegismonitor.backend.demo.DemoDataSeeder;
import com.aegismonitor.backend.demo.DemoSeedResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/demo")
public class DemoSeedController {
    private final DemoDataSeeder demoDataSeeder;

    public DemoSeedController(DemoDataSeeder demoDataSeeder) {
        this.demoDataSeeder = demoDataSeeder;
    }

    @PostMapping("/seed")
    public ApiResponse<DemoSeedResult> seed(@RequestBody DemoSeedHttpRequest request) {
        return ApiResponse.ok(
            "demo data seeded",
            demoDataSeeder.seed(
                request.hostCount(),
                request.includeServices(),
                request.includeAlerts()
            )
        );
    }

    public record DemoSeedHttpRequest(
        int hostCount,
        boolean includeServices,
        boolean includeAlerts
    ) {
    }
}
