package ai.clearowner.controller;

import ai.clearowner.dto.BeneficialOwner;
import ai.clearowner.dto.CompanyDetail;
import ai.clearowner.dto.DirectOwner;
import ai.clearowner.dto.Directorship;
import ai.clearowner.dto.GraphView;
import ai.clearowner.dto.RiskSignals;
import ai.clearowner.service.CompanyService;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/companies")
@Validated
public class CompanyController {

    private final CompanyService companies;

    public CompanyController(CompanyService companies) {
        this.companies = companies;
    }

    @GetMapping("/{companyId}")
    public CompanyDetail detail(@PathVariable String companyId) {
        return companies.detail(companyId);
    }

    /** Shareholders as recorded - one hop only. */
    @GetMapping("/{companyId}/direct-owners")
    public List<DirectOwner> directOwners(@PathVariable String companyId) {
        return companies.directOwners(companyId);
    }

    /** Natural persons whose effective stake, summed over all paths, clears the threshold. */
    @GetMapping("/{companyId}/beneficial-owners")
    public List<BeneficialOwner> beneficialOwners(
            @PathVariable String companyId,
            @RequestParam(defaultValue = "6") @Min(1) @Max(8) int maxDepth,
            @RequestParam(defaultValue = "25.0") @DecimalMin("0.0") @DecimalMax("100.0") double threshold) {
        return companies.beneficialOwners(companyId, maxDepth, threshold);
    }

    @GetMapping("/{companyId}/graph")
    public GraphView graph(@PathVariable String companyId,
                           @RequestParam(defaultValue = "3") @Min(1) @Max(6) int depth) {
        return companies.graph(companyId, depth);
    }

    @GetMapping("/{companyId}/directors")
    public List<Directorship> directors(@PathVariable String companyId) {
        return companies.directors(companyId);
    }

    @GetMapping("/{companyId}/risk-signals")
    public RiskSignals riskSignals(@PathVariable String companyId) {
        return companies.riskSignals(companyId);
    }
}
