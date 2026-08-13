package ai.clearowner.dto;

public record HealthResponse(
        String status,
        boolean databaseReachable,
        long latencyMs,
        String detail) {

    public static HealthResponse up(long latencyMs) {
        return new HealthResponse("UP", true, latencyMs, "CognoDB reachable");
    }

    public static HealthResponse down(String detail) {
        return new HealthResponse("DOWN", false, -1, detail);
    }
}
