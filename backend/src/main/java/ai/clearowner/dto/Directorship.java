package ai.clearowner.dto;

public record Directorship(
        String personId,
        String personName,
        String companyId,
        String companyName,
        String role,
        String appointedOn,
        boolean active) {
}
