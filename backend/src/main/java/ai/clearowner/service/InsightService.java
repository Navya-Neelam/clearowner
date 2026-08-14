package ai.clearowner.service;

import ai.clearowner.dto.AddressCluster;
import ai.clearowner.dto.CircularStructure;
import ai.clearowner.dto.DashboardSummary;
import ai.clearowner.dto.TopController;
import ai.clearowner.repository.InsightRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class InsightService {

    private final InsightRepository insights;

    public InsightService(InsightRepository insights) {
        this.insights = insights;
    }

    public DashboardSummary summary() {
        Map<String, Long> counts = insights.counts();
        return new DashboardSummary(
                counts.getOrDefault("companies", 0L),
                counts.getOrDefault("people", 0L),
                counts.getOrDefault("jurisdictions", 0L),
                counts.getOrDefault("addresses", 0L),
                counts.getOrDefault("ownershipLinks", 0L),
                counts.getOrDefault("directorships", 0L),
                insights.secrecyHavenCompanies(),
                insights.circularStructureCount(),
                insights.sharedAddresses(4, 5),
                insights.topControllers(3, 5));
    }

    public List<CircularStructure> circularStructures(int limit) {
        return insights.circularStructures(limit);
    }

    public List<AddressCluster> sharedAddresses(int minCompanies, int limit) {
        return insights.sharedAddresses(minCompanies, limit);
    }

    public List<TopController> topControllers(int minReach, int limit) {
        return insights.topControllers(minReach, limit);
    }
}
