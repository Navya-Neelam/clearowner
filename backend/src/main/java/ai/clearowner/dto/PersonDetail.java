package ai.clearowner.dto;

public record PersonDetail(
        String personId,
        String name,
        int birthYear,
        boolean pep,
        String nationalityCode,
        String nationalityName,
        int directHoldingCount,
        int directorshipCount) {
}
