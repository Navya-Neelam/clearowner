package ai.clearowner.service;

import ai.clearowner.dto.BeneficialOwner;
import ai.clearowner.dto.CompanyDetail;
import ai.clearowner.dto.DirectOwner;
import ai.clearowner.dto.Directorship;
import ai.clearowner.dto.GraphView;
import ai.clearowner.dto.RiskSignals;
import ai.clearowner.exception.NotFoundException;
import ai.clearowner.repository.CompanyRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CompanyService {

    /**
     * 25% is the reporting threshold used by the EU Anti-Money Laundering
     * Directives and the FATF recommendations, so it is the sensible default for
     * "who counts as a beneficial owner".
     */
    public static final double DEFAULT_THRESHOLD = 25.0;

    private static final int MAX_ROWS = 200;

    private final CompanyRepository companies;

    public CompanyService(CompanyRepository companies) {
        this.companies = companies;
    }

    public CompanyDetail detail(String companyId) {
        return companies.findById(companyId)
                .orElseThrow(() -> new NotFoundException("Company", companyId));
    }

    public List<DirectOwner> directOwners(String companyId) {
        requireExists(companyId);
        return companies.directOwners(companyId, MAX_ROWS);
    }

    public List<BeneficialOwner> beneficialOwners(String companyId, int maxDepth, double threshold) {
        requireExists(companyId);
        return companies.beneficialOwners(companyId, maxDepth, threshold, MAX_ROWS);
    }

    public GraphView graph(String companyId, int depth) {
        requireExists(companyId);
        return companies.subgraph(companyId, depth, MAX_ROWS);
    }

    public List<Directorship> directors(String companyId) {
        requireExists(companyId);
        return companies.directors(companyId, MAX_ROWS);
    }

    /**
     * Structural observations only. Each note describes a shape in the graph; none
     * of them asserts wrongdoing, and the wording in the UI keeps it that way.
     */
    public RiskSignals riskSignals(String companyId) {
        CompanyDetail company = detail(companyId);

        boolean routesThroughHaven = companies.ownershipRoutesThroughSecrecyHaven(companyId);
        boolean circular = companies.partOfCircularStructure(companyId);
        int sameAddress = companies.companiesAtSameAddress(companyId);
        int longestChain = companies.longestOwnershipChain(companyId);
        // Depth 6 rather than 8: the extra two hops cost real time on a small
        // instance and change the answer for almost no company in the dataset.
        int ubos = companies.beneficialOwners(companyId, 6, DEFAULT_THRESHOLD, MAX_ROWS).size();

        List<String> notes = new ArrayList<>();
        if (company.secrecyHaven()) {
            notes.add("Registered in %s, a jurisdiction with limited ownership disclosure."
                    .formatted(company.jurisdictionName()));
        }
        if (routesThroughHaven) {
            notes.add("At least one ownership chain passes through a low-disclosure jurisdiction.");
        }
        if (circular) {
            notes.add("This company sits in a circular ownership structure.");
        }
        if (sameAddress >= 5) {
            notes.add("%d companies share this registered address.".formatted(sameAddress));
        }
        if (longestChain >= 4) {
            notes.add("Ownership is layered %d levels deep before a natural person appears."
                    .formatted(longestChain));
        }
        if (ubos == 0) {
            notes.add("No individual reaches the 25%% threshold through the recorded structure.");
        }
        if (notes.isEmpty()) {
            notes.add("No structural observations for this company.");
        }

        return new RiskSignals(companyId, company.secrecyHaven(), routesThroughHaven, circular,
                sameAddress, longestChain, ubos, notes);
    }

    private void requireExists(String companyId) {
        if (companies.findById(companyId).isEmpty()) {
            throw new NotFoundException("Company", companyId);
        }
    }
}
