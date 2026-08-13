package ai.clearowner.controller;

import ai.clearowner.dto.AddressCluster;
import ai.clearowner.dto.CircularStructure;
import ai.clearowner.dto.DashboardSummary;
import ai.clearowner.dto.TopController;
import ai.clearowner.service.InsightService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@Validated
public class InsightController {

    private final InsightService insights;

    public InsightController(InsightService insights) {
        this.insights = insights;
    }

    @GetMapping("/dashboard/summary")
    public DashboardSummary summary() {
        return insights.summary();
    }

    @GetMapping("/insights/circular-structures")
    public List<CircularStructure> circularStructures(
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int limit) {
        return insights.circularStructures(limit);
    }

    @GetMapping("/insights/shared-addresses")
    public List<AddressCluster> sharedAddresses(
            @RequestParam(defaultValue = "3") @Min(2) @Max(50) int minCompanies,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int limit) {
        return insights.sharedAddresses(minCompanies, limit);
    }

    @GetMapping("/insights/top-controllers")
    public List<TopController> topControllers(
            @RequestParam(defaultValue = "3") @Min(1) @Max(500) int minReach,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int limit) {
        return insights.topControllers(minReach, limit);
    }
}
