package ai.clearowner.dto;

/** A company this person controls, directly or through intermediaries. */
public record Holding(
        String companyId,
        String name,
        String jurisdictionName,
        boolean secrecyHaven,
        double effectivePercentage,
        int shortestPathLength) {
}
