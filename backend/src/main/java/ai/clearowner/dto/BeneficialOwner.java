package ai.clearowner.dto;

/**
 * A natural person's effective stake, summed across every ownership path that
 * reaches the company. {@code routes} is how many distinct paths contributed -
 * anything above one means the register understates their position.
 */
public record BeneficialOwner(
        String personId,
        String name,
        boolean pep,
        double effectivePercentage,
        int routes,
        int shortestPathLength) {
}
