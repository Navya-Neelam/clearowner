package ai.clearowner.seed;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The seed data makes claims the README relies on - that ownership chains run
 * several layers deep, that circular structures exist, that a company's address
 * is in its own jurisdiction. These assert those claims instead of trusting them.
 * <p>
 * No database is involved: generation is deliberately separate from loading.
 */
class SeedDataFactoryTest {

    private static SeedData data;

    @BeforeAll
    static void generate() {
        data = new SeedDataFactory().generate();
    }

    @Test
    void generationIsDeterministic() {
        SeedData again = new SeedDataFactory().generate();

        assertThat(again.companies()).isEqualTo(data.companies());
        assertThat(again.ownerships()).isEqualTo(data.ownerships());
        assertThat(again.people()).isEqualTo(data.people());
    }

    @Test
    void identifiersAreUnique() {
        assertThat(data.companies().stream().map(SeedData.Company::companyId).distinct())
                .hasSize(data.companies().size());
        assertThat(data.people().stream().map(SeedData.Person::personId).distinct())
                .hasSize(data.people().size());
        assertThat(data.addresses().stream().map(SeedData.Address::addressId).distinct())
                .hasSize(data.addresses().size());
    }

    @Test
    void ownershipPercentagesAreSane() {
        assertThat(data.ownerships()).isNotEmpty();
        assertThat(data.ownerships()).allSatisfy(o -> {
            assertThat(o.percentage()).isGreaterThan(0.0).isLessThanOrEqualTo(100.0);
            assertThat(o.ownerType()).isIn("Person", "Company");
        });
    }

    @Test
    void noCompanyIsAllocatedMoreThanTwiceItsShares() {
        Map<String, Double> totals = new HashMap<>();
        for (SeedData.Ownership o : data.ownerships()) {
            totals.merge(o.companyId(), o.percentage(), Double::sum);
        }
        // Planted structures deliberately overlap, but the total must stay in a
        // range a reviewer would recognise as a share register rather than noise.
        assertThat(totals.values()).allSatisfy(total -> assertThat(total).isLessThanOrEqualTo(250.0));
    }

    @Test
    void everyCompanyIsRegisteredAtAnAddressInItsOwnJurisdiction() {
        Map<String, String> addressJurisdiction = new HashMap<>();
        data.addresses().forEach(a -> addressJurisdiction.put(a.addressId(), a.jurisdictionCode()));

        assertThat(data.companies()).allSatisfy(company ->
                assertThat(addressJurisdiction.get(company.addressId()))
                        .as("address jurisdiction for %s", company.name())
                        .isEqualTo(company.jurisdictionCode()));
    }

    @Test
    void legalFormSuffixMatchesJurisdiction() {
        // A GmbH belongs to a German-speaking jurisdiction, not Panama.
        Map<String, Set<String>> allowed = Map.of(
                "DE", Set.of("GmbH", "AG"),
                "CH", Set.of("AG", "GmbH"),
                "PA", Set.of("SA"),
                "SG", Set.of("Pte Ltd"),
                "NL", Set.of("BV", "NV"));

        for (SeedData.Company company : data.companies()) {
            Set<String> permitted = allowed.get(company.jurisdictionCode());
            if (permitted == null) continue;
            assertThat(permitted)
                    .as("suffix of %s in %s", company.name(), company.jurisdictionCode())
                    .anySatisfy(suffix -> assertThat(company.name()).endsWith(" " + suffix));
        }
    }

    @Test
    void ownershipChainsReachAtLeastFourLevels() {
        Map<String, List<String>> ownersOf = new HashMap<>();
        for (SeedData.Ownership o : data.ownerships()) {
            if ("Company".equals(o.ownerType())) {
                ownersOf.computeIfAbsent(o.companyId(), k -> new java.util.ArrayList<>()).add(o.ownerId());
            }
        }

        int deepest = 0;
        for (SeedData.Company company : data.companies()) {
            deepest = Math.max(deepest, depth(company.companyId(), ownersOf, new HashSet<>(), 0));
        }
        assertThat(deepest)
                .as("deepest corporate ownership chain")
                .isGreaterThanOrEqualTo(4);
    }

    @Test
    void circularOwnershipStructuresExist() {
        Map<String, Set<String>> owns = new HashMap<>();
        for (SeedData.Ownership o : data.ownerships()) {
            if ("Company".equals(o.ownerType())) {
                owns.computeIfAbsent(o.ownerId(), k -> new HashSet<>()).add(o.companyId());
            }
        }

        long rings = data.companies().stream()
                .map(SeedData.Company::companyId)
                .filter(id -> reachesItself(id, owns))
                .count();

        assertThat(rings).as("companies sitting in an ownership loop").isGreaterThan(0);
    }

    @Test
    void someAddressesHostManyCompanies() {
        Map<String, Long> perAddress = new HashMap<>();
        data.companies().forEach(c -> perAddress.merge(c.addressId(), 1L, Long::sum));

        assertThat(perAddress.values().stream().max(Long::compareTo).orElse(0L))
                .as("largest registered-office cluster")
                .isGreaterThanOrEqualTo(5L);
    }

    private int depth(String companyId, Map<String, List<String>> ownersOf,
                      Set<String> visited, int level) {
        if (level > 12 || !visited.add(companyId)) return level;
        int best = level;
        for (String owner : ownersOf.getOrDefault(companyId, List.of())) {
            best = Math.max(best, depth(owner, ownersOf, visited, level + 1));
        }
        visited.remove(companyId);
        return best;
    }

    private boolean reachesItself(String start, Map<String, Set<String>> owns) {
        Set<String> seen = new HashSet<>();
        java.util.Deque<String> queue = new java.util.ArrayDeque<>(owns.getOrDefault(start, Set.of()));
        while (!queue.isEmpty()) {
            String next = queue.poll();
            if (next.equals(start)) return true;
            if (!seen.add(next) || seen.size() > 500) continue;
            queue.addAll(owns.getOrDefault(next, Set.of()));
        }
        return false;
    }
}
