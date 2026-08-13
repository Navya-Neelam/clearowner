package ai.clearowner.seed;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Builds a deterministic synthetic ownership graph.
 * <p>
 * Companies are assigned an ownership tier. A tier-0 company is an operating
 * business; higher tiers are holding companies that sit above it. Owners are
 * drawn from the tier above, so chains form naturally and reach five hops
 * before a natural person is found - which is what makes the beneficial
 * ownership query worth running.
 * <p>
 * On top of that random-but-structured base we plant specific shapes the
 * application needs to demonstrate: circular ownership, diamond structures
 * where one person reaches a company by several routes, and address clusters
 * where many shell companies share one registered office.
 * <p>
 * The seed is fixed, so the same dataset is produced on every run.
 */
@Component
public class SeedDataFactory {

    private static final long SEED = 42L;

    private static final int[] TIER_SIZES = {156, 70, 40, 22, 12}; // tier 0 .. tier 4
    private static final int PEOPLE = 400;
    private static final int ADDRESSES = 250;

    private static final String[][] JURISDICTIONS = {
            // code, name, riskRating, secrecyHaven
            {"GB", "United Kingdom", "LOW", "false"},
            {"IE", "Ireland", "LOW", "false"},
            {"DE", "Germany", "LOW", "false"},
            {"FR", "France", "LOW", "false"},
            {"NL", "Netherlands", "MEDIUM", "false"},
            {"US", "United States", "LOW", "false"},
            {"CA", "Canada", "LOW", "false"},
            {"IN", "India", "MEDIUM", "false"},
            {"SG", "Singapore", "MEDIUM", "false"},
            {"AE", "United Arab Emirates", "MEDIUM", "false"},
            {"CH", "Switzerland", "MEDIUM", "false"},
            {"LU", "Luxembourg", "MEDIUM", "true"},
            {"CY", "Cyprus", "HIGH", "true"},
            {"MT", "Malta", "HIGH", "true"},
            {"VG", "British Virgin Islands", "HIGH", "true"},
            {"KY", "Cayman Islands", "HIGH", "true"},
            {"PA", "Panama", "HIGH", "true"},
            {"SC", "Seychelles", "HIGH", "true"},
            {"BZ", "Belize", "HIGH", "true"},
            {"JE", "Jersey", "HIGH", "true"},
    };

    private static final String[] CO_FIRST = {
            "Halcyon", "Meridian", "Northgate", "Ravenswood", "Silverline", "Blackwater",
            "Kestrel", "Orion", "Sable", "Thornbury", "Vanguard", "Westbrook", "Ashford",
            "Bramble", "Cobalt", "Dunmore", "Everest", "Foxglove", "Granite", "Harrow",
            "Ironvale", "Juniper", "Kingsway", "Lattice", "Marlowe", "Nimbus", "Oakhurst",
            "Pinnacle", "Quarry", "Redstone", "Stonebridge", "Tamarind", "Umber", "Verdant",
            "Whitfield", "Yarrow", "Zephyr", "Aldgate", "Bexley", "Carrow",
    };

    private static final String[] CO_SECOND = {
            "Trading", "Holdings", "Capital", "Ventures", "Partners", "Industries",
            "Logistics", "Maritime", "Resources", "Property", "Energy", "Consulting",
            "Investments", "Commodities", "Textiles", "Foods", "Media", "Systems",
            "Pharma", "Agri", "Marine", "Metals", "Freight", "Chemicals",
    };

    private static final String[] CO_SUFFIX = {"Ltd", "PLC", "GmbH", "S.a.r.l", "Pte Ltd", "Inc", "BV", "SA"};

    private static final String[] FIRST_NAMES = {
            "Amara", "Bjorn", "Carmen", "Dmitri", "Elena", "Farid", "Greta", "Hassan",
            "Imogen", "Jozef", "Kiran", "Lucia", "Mateo", "Nadia", "Oskar", "Priya",
            "Quentin", "Rania", "Stefan", "Tomas", "Ulrika", "Viktor", "Wren", "Xavier",
            "Yasmin", "Zara", "Adrian", "Beatriz", "Cormac", "Dilara", "Emeka", "Freya",
            "Gustav", "Helena", "Ivan", "Johanna", "Karim", "Liam", "Marta", "Niamh",
    };

    private static final String[] LAST_NAMES = {
            "Achterberg", "Bellamy", "Castellanos", "Dumitrescu", "Eriksen", "Fontaine",
            "Grimaldi", "Halvorsen", "Ibrahim", "Jankowski", "Kowalczyk", "Lindqvist",
            "Moretti", "Novak", "Okonkwo", "Petrova", "Quintero", "Rasmussen", "Sorensen",
            "Tanaka", "Ustinov", "Vasquez", "Wintersen", "Ximenes", "Yildirim", "Zielinski",
            "Andersson", "Brennan", "Cioffi", "Delacroix", "Espinoza", "Fitzgerald",
    };

    /** Index-aligned with JURISDICTIONS, so an address city always matches its country. */
    private static final String[] CITIES = {
            "London", "Dublin", "Frankfurt", "Paris", "Amsterdam", "New York", "Toronto",
            "Mumbai", "Singapore", "Dubai", "Zurich", "Luxembourg", "Limassol", "Valletta",
            "Road Town", "George Town", "Panama City", "Victoria", "Belize City", "Saint Helier",
    };

    /** Also index-aligned with JURISDICTIONS - a plausible street for each capital. */
    private static final String[] STREETS = {
            "Fenchurch Street", "Dame Street", "Alte Gasse", "Rue Lafayette", "Keizersgracht",
            "Broad Street", "Bay Street", "Marine Drive", "Raffles Quay", "Sheikh Zayed Road",
            "Bahnhofstrasse", "Boulevard Royal", "Arch. Makariou", "Republic Street",
            "Waterfront Drive", "Elgin Avenue", "Calle 50", "Independence Avenue",
            "Albert Street", "The Esplanade",
    };

    private static final String[] ROLES = {
            "Director", "Managing Director", "Company Secretary", "Non-Executive Director",
            "Chairperson", "Nominee Director",
    };

    private static final String[] SHARE_CLASSES = {"Ordinary", "Ordinary", "Ordinary", "A", "B", "Preference"};

    public SeedData generate() {
        Random rnd = new Random(SEED);

        List<SeedData.Jurisdiction> jurisdictions = buildJurisdictions();
        List<String> codes = jurisdictions.stream().map(SeedData.Jurisdiction::code).toList();
        List<String> havenCodes = jurisdictions.stream()
                .filter(SeedData.Jurisdiction::secrecyHaven)
                .map(SeedData.Jurisdiction::code).toList();

        List<SeedData.Address> addresses = buildAddresses(rnd, codes);
        List<SeedData.Person> people = buildPeople(rnd, codes);
        List<List<SeedData.Company>> tiers = buildCompanies(rnd, codes, havenCodes, addresses);

        List<SeedData.Company> companies = new ArrayList<>();
        tiers.forEach(companies::addAll);

        List<SeedData.Ownership> ownerships = buildOwnerships(rnd, tiers, people);
        plantCircularOwnership(tiers, ownerships);
        plantDiamondStructures(rnd, tiers, people, ownerships);

        List<SeedData.Directorship> directorships = buildDirectorships(rnd, companies, people);

        return new SeedData(jurisdictions, addresses, people, companies, ownerships, directorships);
    }

    private List<SeedData.Jurisdiction> buildJurisdictions() {
        List<SeedData.Jurisdiction> out = new ArrayList<>();
        for (String[] j : JURISDICTIONS) {
            out.add(new SeedData.Jurisdiction(j[0], j[1], j[2], Boolean.parseBoolean(j[3])));
        }
        return out;
    }

    /**
     * Most addresses host a single company. A handful are deliberately reused by
     * many companies - the registered-office clusters the Insights screen surfaces.
     */
    private List<SeedData.Address> buildAddresses(Random rnd, List<String> codes) {
        List<SeedData.Address> out = new ArrayList<>();
        for (int i = 0; i < ADDRESSES; i++) {
            // Pick the jurisdiction first, then its city, so the two always agree.
            int jurisdiction = rnd.nextInt(codes.size());
            String city = CITIES[jurisdiction];
            out.add(new SeedData.Address(
                    "ADDR-%04d".formatted(i + 1),
                    "%d %s".formatted(1 + rnd.nextInt(240), STREETS[jurisdiction]),
                    city,
                    "%s%d".formatted(city.replace(" ", "").substring(0, 2).toUpperCase(),
                            10000 + rnd.nextInt(89999)),
                    codes.get(jurisdiction)));
        }
        return out;
    }

    private List<SeedData.Person> buildPeople(Random rnd, List<String> codes) {
        List<SeedData.Person> out = new ArrayList<>();
        Set<String> used = new LinkedHashSet<>();
        for (int i = 0; i < PEOPLE; i++) {
            String name;
            do {
                name = FIRST_NAMES[rnd.nextInt(FIRST_NAMES.length)] + " "
                        + LAST_NAMES[rnd.nextInt(LAST_NAMES.length)];
            } while (!used.add(name));
            out.add(new SeedData.Person(
                    "PER-%04d".formatted(i + 1),
                    name,
                    1945 + rnd.nextInt(50),
                    codes.get(rnd.nextInt(codes.size())),
                    rnd.nextInt(100) < 7));
        }
        return out;
    }

    /**
     * Higher-tier companies are holding vehicles and are far more likely to sit in
     * a secrecy jurisdiction, which is what makes the jurisdiction-risk query
     * produce a meaningful answer rather than noise.
     */
    private List<List<SeedData.Company>> buildCompanies(Random rnd, List<String> codes,
                                                        List<String> havenCodes,
                                                        List<SeedData.Address> addresses) {
        // A few addresses act as company mills hosting many registrations.
        List<SeedData.Address> mills = addresses.subList(0, 8);

        List<List<SeedData.Company>> tiers = new ArrayList<>();
        Set<String> usedNames = new LinkedHashSet<>();
        int seq = 0;

        for (int tier = 0; tier < TIER_SIZES.length; tier++) {
            List<SeedData.Company> tierCompanies = new ArrayList<>();
            for (int i = 0; i < TIER_SIZES[tier]; i++) {
                seq++;
                String name;
                do {
                    name = "%s %s %s".formatted(
                            CO_FIRST[rnd.nextInt(CO_FIRST.length)],
                            CO_SECOND[rnd.nextInt(CO_SECOND.length)],
                            CO_SUFFIX[rnd.nextInt(CO_SUFFIX.length)]);
                } while (!usedNames.add(name));

                boolean holding = tier > 0;
                String jurisdiction = holding && rnd.nextInt(100) < 55
                        ? havenCodes.get(rnd.nextInt(havenCodes.size()))
                        : codes.get(rnd.nextInt(codes.size()));

                // Holding companies cluster into the mill addresses; operating ones rarely do.
                SeedData.Address address = (holding && rnd.nextInt(100) < 45)
                        ? mills.get(rnd.nextInt(mills.size()))
                        : addresses.get(rnd.nextInt(addresses.size()));

                tierCompanies.add(new SeedData.Company(
                        "CO-%04d".formatted(seq),
                        name,
                        rnd.nextInt(100) < 92 ? "ACTIVE" : "DISSOLVED",
                        "%d-%02d-%02d".formatted(1985 + rnd.nextInt(38), 1 + rnd.nextInt(12), 1 + rnd.nextInt(28)),
                        holding ? "Holding Company" : "Private Limited Company",
                        jurisdiction,
                        address.addressId()));
            }
            tiers.add(tierCompanies);
        }
        return tiers;
    }

    /**
     * Each company draws its owners from the tier above; the top tier is owned by
     * natural persons. Percentages never quite reach 100 - real registers carry an
     * unallocated remainder, and the UI is explicit that this is expected.
     */
    private List<SeedData.Ownership> buildOwnerships(Random rnd, List<List<SeedData.Company>> tiers,
                                                     List<SeedData.Person> people) {
        List<SeedData.Ownership> out = new ArrayList<>();

        for (int tier = 0; tier < tiers.size(); tier++) {
            boolean topTier = tier == tiers.size() - 1;
            List<SeedData.Company> parents = topTier ? List.of() : tiers.get(tier + 1);

            for (SeedData.Company company : tiers.get(tier)) {
                int ownerCount = 1 + rnd.nextInt(3);
                double remaining = 100.0;
                Set<String> seen = new LinkedHashSet<>();

                for (int i = 0; i < ownerCount && remaining > 5; i++) {
                    boolean last = i == ownerCount - 1;
                    double pct = last
                            ? round2(remaining * (0.6 + rnd.nextDouble() * 0.4))
                            : round2(remaining * (0.25 + rnd.nextDouble() * 0.45));
                    if (pct < 1) continue;
                    remaining = round2(remaining - pct);

                    String ownerId;
                    String ownerType;
                    if (!topTier && rnd.nextInt(100) < 70) {
                        ownerId = parents.get(rnd.nextInt(parents.size())).companyId();
                        ownerType = "Company";
                    } else {
                        ownerId = people.get(rnd.nextInt(people.size())).personId();
                        ownerType = "Person";
                    }
                    if (!seen.add(ownerType + ownerId)) continue;

                    out.add(new SeedData.Ownership(ownerId, ownerType, company.companyId(), pct,
                            SHARE_CLASSES[rnd.nextInt(SHARE_CLASSES.length)],
                            "%d-%02d-%02d".formatted(1995 + rnd.nextInt(29), 1 + rnd.nextInt(12), 1 + rnd.nextInt(28))));
                }
            }
        }
        return out;
    }

    /**
     * Circular ownership - A owns B owns C owns A - cannot arise from the tier
     * rules, so the rings are added explicitly. They are a genuine red flag in
     * corporate structures and have no natural relational equivalent.
     */
    private void plantCircularOwnership(List<List<SeedData.Company>> tiers,
                                        List<SeedData.Ownership> out) {
        List<SeedData.Company> holdings = new ArrayList<>();
        for (int t = 1; t < tiers.size(); t++) holdings.addAll(tiers.get(t));

        int[][] rings = {{0, 1, 2}, {7, 8, 9}, {14, 15, 16}, {21, 22, 23, 24}, {31, 32}};
        for (int[] ring : rings) {
            for (int i = 0; i < ring.length; i++) {
                SeedData.Company owner = holdings.get(ring[i]);
                SeedData.Company owned = holdings.get(ring[(i + 1) % ring.length]);
                out.add(new SeedData.Ownership(owner.companyId(), "Company", owned.companyId(),
                        12.0 + i * 3, "Ordinary", "2019-06-%02d".formatted(1 + i)));
            }
        }
    }

    /**
     * A diamond gives one person two independent routes to the same company, so
     * their effective stake is the sum of both paths. This is the case that makes
     * the "who really controls this" number differ most from the register.
     */
    private void plantDiamondStructures(Random rnd, List<List<SeedData.Company>> tiers,
                                        List<SeedData.Person> people,
                                        List<SeedData.Ownership> out) {
        List<SeedData.Company> tier0 = tiers.get(0);
        List<SeedData.Company> tier1 = tiers.get(1);
        List<SeedData.Company> tier2 = tiers.get(2);

        for (int d = 0; d < 12; d++) {
            SeedData.Person controller = people.get(d * 7 % people.size());
            SeedData.Company left = tier1.get((d * 3) % tier1.size());
            SeedData.Company right = tier1.get((d * 3 + 1) % tier1.size());
            SeedData.Company target = tier0.get((d * 11) % tier0.size());
            SeedData.Company apex = tier2.get(d % tier2.size());

            out.add(new SeedData.Ownership(left.companyId(), "Company", target.companyId(),
                    30.0 + rnd.nextInt(15), "Ordinary", "2017-03-14"));
            out.add(new SeedData.Ownership(right.companyId(), "Company", target.companyId(),
                    20.0 + rnd.nextInt(15), "Ordinary", "2018-09-02"));
            out.add(new SeedData.Ownership(apex.companyId(), "Company", left.companyId(),
                    55.0 + rnd.nextInt(20), "Ordinary", "2016-01-20"));
            out.add(new SeedData.Ownership(apex.companyId(), "Company", right.companyId(),
                    45.0 + rnd.nextInt(25), "Ordinary", "2016-01-20"));
            out.add(new SeedData.Ownership(controller.personId(), "Person", apex.companyId(),
                    60.0 + rnd.nextInt(30), "Ordinary", "2015-11-08"));
        }
    }

    private List<SeedData.Directorship> buildDirectorships(Random rnd, List<SeedData.Company> companies,
                                                           List<SeedData.Person> people) {
        List<SeedData.Directorship> out = new ArrayList<>();
        // A small pool of nominee directors sits on many boards, which is itself a signal.
        List<SeedData.Person> nominees = people.subList(0, 12);

        for (SeedData.Company company : companies) {
            int count = 1 + rnd.nextInt(3);
            Set<String> seen = new LinkedHashSet<>();
            for (int i = 0; i < count; i++) {
                SeedData.Person person = rnd.nextInt(100) < 22
                        ? nominees.get(rnd.nextInt(nominees.size()))
                        : people.get(rnd.nextInt(people.size()));
                if (!seen.add(person.personId())) continue;
                out.add(new SeedData.Directorship(person.personId(), company.companyId(),
                        ROLES[rnd.nextInt(ROLES.length)],
                        "%d-%02d-%02d".formatted(2005 + rnd.nextInt(19), 1 + rnd.nextInt(12), 1 + rnd.nextInt(28)),
                        rnd.nextInt(100) < 88));
            }
        }
        return out;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
