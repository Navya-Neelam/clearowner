package ai.clearowner.repository;

import ai.clearowner.dto.AddressCluster;
import ai.clearowner.dto.CircularStructure;
import ai.clearowner.dto.TopController;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Values;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class InsightRepository {

    private final Driver driver;

    public InsightRepository(Driver driver) {
        this.driver = driver;
    }

    public Map<String, Long> counts() {
        String cypher = """
                MATCH (c:Company)      WITH count(c) AS companies
                MATCH (p:Person)       WITH companies, count(p) AS people
                MATCH (j:Jurisdiction) WITH companies, people, count(j) AS jurisdictions
                MATCH (a:Address)      WITH companies, people, jurisdictions, count(a) AS addresses
                MATCH ()-[o:OWNS]->()  WITH companies, people, jurisdictions, addresses, count(o) AS ownershipLinks
                MATCH ()-[d:DIRECTOR_OF]->()
                RETURN companies, people, jurisdictions, addresses, ownershipLinks,
                       count(d) AS directorships
                """;
        try (var session = driver.session()) {
            return session.executeRead(tx -> {
                var rows = tx.run(cypher).list();
                if (rows.isEmpty()) return Map.<String, Long>of();
                var r = rows.get(0);
                return Map.of(
                        "companies", r.get("companies").asLong(),
                        "people", r.get("people").asLong(),
                        "jurisdictions", r.get("jurisdictions").asLong(),
                        "addresses", r.get("addresses").asLong(),
                        "ownershipLinks", r.get("ownershipLinks").asLong(),
                        "directorships", r.get("directorships").asLong());
            });
        }
    }

    /** Counts rings without building the ring objects, which the dashboard does not need. */
    public long circularStructureCount() {
        String twoWay = """
                MATCH (a:Company)-[:OWNS]->(b:Company)-[:OWNS]->(z:Company)
                WHERE z.companyId = a.companyId AND a.companyId < b.companyId
                RETURN count(*) AS total
                """;
        String threeWay = """
                MATCH (a:Company)-[:OWNS]->(b:Company)-[:OWNS]->(c:Company)-[:OWNS]->(z:Company)
                WHERE z.companyId = a.companyId
                  AND a.companyId < b.companyId AND a.companyId < c.companyId
                RETURN count(*) AS total
                """;
        String fourWay = """
                MATCH (a:Company)-[:OWNS]->(b:Company)-[:OWNS]->(c:Company)-[:OWNS]->(d:Company)-[:OWNS]->(z:Company)
                WHERE z.companyId = a.companyId
                  AND a.companyId < b.companyId AND a.companyId < c.companyId
                  AND a.companyId < d.companyId
                RETURN count(*) AS total
                """;
        try (var session = driver.session()) {
            return session.executeRead(tx -> {
                long total = 0;
                for (String cypher : List.of(twoWay, threeWay, fourWay)) {
                    var rows = tx.run(cypher).list();
                    if (!rows.isEmpty()) total += rows.get(0).get("total").asLong(0);
                }
                return total;
            });
        }
    }

    public long secrecyHavenCompanies() {
        try (var session = driver.session()) {
            return session.executeRead(tx -> tx.run(
                            "MATCH (c:Company) WHERE c.secrecyHaven = true RETURN count(c) AS total")
                    .list().get(0).get("total").asLong());
        }
    }

    /**
     * Closed ownership loops.
     * <p>
     * CognoDB's variable-length expansion will not revisit a node it has already
     * traversed, so {@code (a)-[:OWNS*2..4]->(a)} never matches. Rings are found
     * with explicit fixed-length patterns instead, one query per ring size.
     * <p>
     * Each ring would otherwise be reported once per starting member, so the
     * lowest company id is required to come first - a canonical rotation.
     */
    public List<CircularStructure> circularStructures(int limit) {
        String twoWay = """
                MATCH (a:Company)-[r1:OWNS]->(b:Company)-[r2:OWNS]->(z:Company)
                WHERE z.companyId = a.companyId AND a.companyId < b.companyId
                RETURN a.companyId AS aId, a.name AS aName, r1.percentage AS aPct,
                       b.companyId AS bId, b.name AS bName, r2.percentage AS bPct,
                       null AS cId, null AS cName, null AS cPct
                LIMIT $limit
                """;
        String threeWay = """
                MATCH (a:Company)-[r1:OWNS]->(b:Company)-[r2:OWNS]->(c:Company)-[r3:OWNS]->(z:Company)
                WHERE z.companyId = a.companyId
                  AND a.companyId < b.companyId AND a.companyId < c.companyId
                RETURN a.companyId AS aId, a.name AS aName, r1.percentage AS aPct,
                       b.companyId AS bId, b.name AS bName, r2.percentage AS bPct,
                       c.companyId AS cId, c.name AS cName, r3.percentage AS cPct,
                       null AS dId, null AS dName, null AS dPct
                LIMIT $limit
                """;
        String fourWay = """
                MATCH (a:Company)-[r1:OWNS]->(b:Company)-[r2:OWNS]->(c:Company)-[r3:OWNS]->(d:Company)-[r4:OWNS]->(z:Company)
                WHERE z.companyId = a.companyId
                  AND a.companyId < b.companyId AND a.companyId < c.companyId
                  AND a.companyId < d.companyId
                RETURN a.companyId AS aId, a.name AS aName, r1.percentage AS aPct,
                       b.companyId AS bId, b.name AS bName, r2.percentage AS bPct,
                       c.companyId AS cId, c.name AS cName, r3.percentage AS cPct,
                       d.companyId AS dId, d.name AS dName, r4.percentage AS dPct
                LIMIT $limit
                """;

        List<CircularStructure> out = new ArrayList<>();
        try (var session = driver.session()) {
            session.executeRead(tx -> {
                for (String cypher : List.of(twoWay, threeWay, fourWay)) {
                    for (var r : tx.run(cypher, Map.of("limit", Values.value(limit))).list()) {
                        List<CircularStructure.Member> members = new ArrayList<>();
                        members.add(new CircularStructure.Member(
                                r.get("aId").asString(), r.get("aName").asString(),
                                Cypher.round2(r.get("aPct").asDouble(0))));
                        members.add(new CircularStructure.Member(
                                r.get("bId").asString(), r.get("bName").asString(),
                                Cypher.round2(r.get("bPct").asDouble(0))));
                        if (!r.get("cId").isNull()) {
                            members.add(new CircularStructure.Member(
                                    r.get("cId").asString(), r.get("cName").asString(),
                                    Cypher.round2(r.get("cPct").asDouble(0))));
                        }
                        if (!r.get("dId").isNull()) {
                            members.add(new CircularStructure.Member(
                                    r.get("dId").asString(), r.get("dName").asString(),
                                    Cypher.round2(r.get("dPct").asDouble(0))));
                        }
                        out.add(new CircularStructure(members.size(), members));
                    }
                }
                return null;
            });
        }
        return out;
    }

    /**
     * Registered-office clusters. Address is a node rather than a property on
     * Company precisely so this is a traversal - in a relational schema it would
     * be a self-join on a free-text column.
     */
    public List<AddressCluster> sharedAddresses(int minCompanies, int limit) {
        String cypher = """
                MATCH (a:Address)<-[:REGISTERED_AT]-(c:Company)
                WITH a, count(c) AS companyCount, collect({id: c.companyId, name: c.name}) AS companies
                WHERE companyCount >= $minCompanies
                OPTIONAL MATCH (a)-[:LOCATED_IN]->(j:Jurisdiction)
                RETURN a.addressId AS addressId, a.line AS line, a.city AS city,
                       j.name AS jurisdictionName, companyCount, companies[0..12] AS sample
                ORDER BY companyCount DESC
                LIMIT $limit
                """;
        try (var session = driver.session()) {
            return session.executeRead(tx -> tx.run(cypher, Map.of(
                            "minCompanies", Values.value(minCompanies),
                            "limit", Values.value(limit)))
                    .list(r -> {
                        List<AddressCluster.CompanyRef> refs = new ArrayList<>();
                        for (var v : r.get("sample").values()) {
                            refs.add(new AddressCluster.CompanyRef(
                                    v.get("id").asString(), v.get("name").asString()));
                        }
                        return new AddressCluster(
                                r.get("addressId").asString(),
                                r.get("line").asString(""),
                                r.get("city").asString(""),
                                r.get("jurisdictionName").asString(""),
                                r.get("companyCount").asInt(),
                                refs);
                    }));
        }
    }

    /** People whose ownership reaches an unusually wide set of companies. */
    public List<TopController> topControllers(int minReach, int limit) {
        String cypher = """
                MATCH path = (p:Person)-[:OWNS*1..5]->(c:Company)
                WITH p, c, min(length(path)) AS hops
                WITH p, count(c) AS companiesReached, max(hops) AS maxDepth
                WHERE companiesReached >= $minReach
                RETURN p.personId AS personId, p.name AS name, p.pep AS pep,
                       companiesReached, maxDepth
                ORDER BY companiesReached DESC
                LIMIT $limit
                """;
        try (var session = driver.session()) {
            return session.executeRead(tx -> tx.run(cypher, Map.of(
                            "minReach", Values.value(minReach),
                            "limit", Values.value(limit)))
                    .list(r -> new TopController(
                            r.get("personId").asString(),
                            r.get("name").asString(),
                            r.get("pep").asBoolean(false),
                            r.get("companiesReached").asInt(),
                            r.get("maxDepth").asInt())));
        }
    }
}
