package ai.clearowner.dto;

import java.util.List;

/**
 * Structural observations about a company. These describe the shape of the
 * ownership graph - they are not accusations and carry no verdict.
 */
public record RiskSignals(
        String companyId,
        boolean registeredInSecrecyHaven,
        boolean ownershipRoutesThroughSecrecyHaven,
        boolean partOfCircularStructure,
        int companiesAtSameAddress,
        int longestOwnershipChain,
        int beneficialOwnersAboveThreshold,
        List<String> notes) {
}
