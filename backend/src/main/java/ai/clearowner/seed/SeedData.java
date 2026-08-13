package ai.clearowner.seed;

import java.util.List;

/**
 * The complete synthetic dataset, generated in memory before any write occurs.
 * Keeping generation separate from loading makes the shape of the data easy to
 * test and reason about without a database.
 */
public record SeedData(
        List<Jurisdiction> jurisdictions,
        List<Address> addresses,
        List<Person> people,
        List<Company> companies,
        List<Ownership> ownerships,
        List<Directorship> directorships) {

    public record Jurisdiction(String code, String name, String riskRating, boolean secrecyHaven) {
    }

    public record Address(String addressId, String line, String city, String postalCode,
                          String jurisdictionCode) {
    }

    public record Person(String personId, String name, int birthYear, String nationality,
                         boolean pep) {
    }

    public record Company(String companyId, String name, String status, String incorporationDate,
                          String companyType, String jurisdictionCode, String addressId) {
    }

    /** ownerType is either "Person" or "Company" - it selects which label to match on write. */
    public record Ownership(String ownerId, String ownerType, String companyId, double percentage,
                            String shareClass, String since) {
    }

    public record Directorship(String personId, String companyId, String role, String appointedOn,
                               boolean active) {
    }
}
