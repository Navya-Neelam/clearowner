package ai.clearowner.dto;

/** A person whose ownership reaches an unusually large number of companies. */
public record TopController(
        String personId,
        String name,
        boolean pep,
        int companiesReached,
        int maxDepth) {
}
