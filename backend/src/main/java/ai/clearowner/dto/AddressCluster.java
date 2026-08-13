package ai.clearowner.dto;

import java.util.List;

/** Companies sharing one registered office - a common shell-company pattern. */
public record AddressCluster(
        String addressId,
        String line,
        String city,
        String jurisdictionName,
        int companyCount,
        List<CompanyRef> companies) {

    public record CompanyRef(String companyId, String name) {
    }
}
