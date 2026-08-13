package ai.clearowner.controller;

import ai.clearowner.service.HealthService;
import ai.clearowner.dto.HealthResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    /**
     * Reports whether the API can actually reach CognoDB. Returns 503 when it
     * cannot, so the frontend can show a database-unavailable state instead of
     * failing silently on every page.
     */
    @GetMapping
    public ResponseEntity<HealthResponse> health() {
        HealthResponse health = healthService.check();
        return health.databaseReachable()
                ? ResponseEntity.ok(health)
                : ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(health);
    }
}
