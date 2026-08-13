package ai.clearowner.service;

import ai.clearowner.dto.HealthResponse;
import org.neo4j.driver.Driver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class HealthService {

    private static final Logger log = LoggerFactory.getLogger(HealthService.class);

    private final Driver driver;

    public HealthService(Driver driver) {
        this.driver = driver;
    }

    public HealthResponse check() {
        long start = System.currentTimeMillis();
        try (var session = driver.session()) {
            session.run("RETURN 1").consume();
            return HealthResponse.up(System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.warn("CognoDB health check failed: {}", e.getMessage());
            return HealthResponse.down(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}
