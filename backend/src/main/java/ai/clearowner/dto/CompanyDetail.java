package ai.clearowner.dto;

public record CompanyDetail(
        String companyId,
        String name,
        String status,
        String companyType,
        String incorporationDate,
        String jurisdictionCode,
        String jurisdictionName,
        boolean secrecyHaven,
        String addressLine,
        String addressCity,
        int directOwnerCount,
        int subsidiaryCount,
        int directorCount) {
}
