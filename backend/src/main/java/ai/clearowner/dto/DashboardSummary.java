package ai.clearowner.dto;

import java.util.List;

public record DashboardSummary(
        long companies,
        long people,
        long jurisdictions,
        long addresses,
        long ownershipLinks,
        long directorships,
        long secrecyHavenCompanies,
        long circularStructures,
        List<AddressCluster> topAddressClusters,
        List<TopController> topControllers) {
}
