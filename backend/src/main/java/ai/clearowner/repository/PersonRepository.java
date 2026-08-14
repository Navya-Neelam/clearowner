package ai.clearowner.repository;

import ai.clearowner.dto.Directorship;
import ai.clearowner.dto.Holding;
import ai.clearowner.dto.PersonDetail;
import ai.clearowner.dto.SearchResult;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Values;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class PersonRepository {

    private final Driver driver;

    public PersonRepository(Driver driver) {
        this.driver = driver;
    }

    /** Cheap existence check, for the same reason as the company one. */
    public boolean exists(String personId) {
        String cypher = "MATCH (p:Person {personId: $personId}) RETURN count(p) AS found";
        try (var session = driver.session()) {
            return session.executeRead(tx -> {
                var rows = tx.run(cypher, Map.of("personId", personId)).list();
                return !rows.isEmpty() && rows.get(0).get("found").asInt(0) > 0;
            });
        }
    }

    public Optional<PersonDetail> findById(String personId) {
        String cypher = """
                MATCH (p:Person {personId: $personId})
                OPTIONAL MATCH (p)-[:RESIDENT_OF]->(j:Jurisdiction)
                RETURN p.personId AS personId,
                       p.name      AS name,
                       p.birthYear AS birthYear,
                       p.pep       AS pep,
                       j.code      AS nationalityCode,
                       j.name      AS nationalityName,
                       COUNT { (p)-[:OWNS]->() }        AS directHoldingCount,
                       COUNT { (p)-[:DIRECTOR_OF]->() } AS directorshipCount
                """;
        try (var session = driver.session()) {
            return session.executeRead(tx -> tx.run(cypher, Map.of("personId", personId))
                    .list(r -> new PersonDetail(
                            r.get("personId").asString(),
                            r.get("name").asString(),
                            r.get("birthYear").asInt(0),
                            r.get("pep").asBoolean(false),
                            r.get("nationalityCode").asString(""),
                            r.get("nationalityName").asString(""),
                            r.get("directHoldingCount").asInt(),
                            r.get("directorshipCount").asInt())))
                    .stream().findFirst();
        }
    }

    /**
     * The beneficial-ownership calculation run from the other direction: every
     * company this person reaches, with the stake they effectively hold in it.
     */
    public List<Holding> holdings(String personId, int maxDepth, double threshold, int limit) {
        String cypher = """
                MATCH path = (p:Person {personId: $personId})-[:OWNS*1..8]->(c:Company)
                WHERE length(path) <= $maxDepth
                WITH c,
                     reduce(share = 1.0, r IN relationships(path) | share * r.percentage / 100.0) AS pathShare,
                     length(path) AS hops
                WITH c,
                     sum(pathShare) * 100.0 AS effectivePercentage,
                     min(hops)              AS shortestPathLength
                WHERE effectivePercentage >= $threshold
                RETURN c.companyId        AS companyId,
                       c.name             AS name,
                       c.jurisdictionName AS jurisdictionName,
                       c.secrecyHaven     AS secrecyHaven,
                       effectivePercentage,
                       shortestPathLength
                ORDER BY effectivePercentage DESC
                LIMIT $limit
                """;
        try (var session = driver.session()) {
            return session.executeRead(tx -> tx.run(cypher, Map.of(
                            "personId", personId,
                            "maxDepth", Values.value(maxDepth),
                            "threshold", Values.value(threshold),
                            "limit", Values.value(limit)))
                    .list(r -> new Holding(
                            r.get("companyId").asString(),
                            r.get("name").asString(),
                            r.get("jurisdictionName").asString(""),
                            r.get("secrecyHaven").asBoolean(false),
                            Cypher.round2(r.get("effectivePercentage").asDouble()),
                            r.get("shortestPathLength").asInt())));
        }
    }

    public List<Directorship> directorships(String personId, int limit) {
        String cypher = """
                MATCH (p:Person {personId: $personId})-[r:DIRECTOR_OF]->(c:Company)
                RETURN p.personId AS personId, p.name AS personName,
                       c.companyId AS companyId, c.name AS companyName,
                       r.role AS role, r.appointedOn AS appointedOn, r.active AS active
                ORDER BY r.active DESC, c.name
                LIMIT $limit
                """;
        try (var session = driver.session()) {
            return session.executeRead(tx -> tx.run(cypher,
                            Map.of("personId", personId, "limit", Values.value(limit)))
                    .list(r -> new Directorship(
                            r.get("personId").asString(),
                            r.get("personName").asString(),
                            r.get("companyId").asString(),
                            r.get("companyName").asString(),
                            r.get("role").asString(""),
                            r.get("appointedOn").asString(""),
                            r.get("active").asBoolean(false))));
        }
    }

    public List<SearchResult> search(String query, int limit) {
        String cypher = """
                MATCH (p:Person)
                WHERE toLower(p.name) CONTAINS toLower($query)
                RETURN 'Person' AS type, p.personId AS id, p.name AS name,
                       CASE WHEN p.pep THEN 'Politically exposed person' ELSE '' END AS subtitle
                ORDER BY p.name
                LIMIT $limit
                """;
        try (var session = driver.session()) {
            return session.executeRead(tx -> tx.run(cypher,
                            Map.of("query", query, "limit", Values.value(limit)))
                    .list(r -> new SearchResult(
                            r.get("type").asString(),
                            r.get("id").asString(),
                            r.get("name").asString(),
                            r.get("subtitle").asString(""))));
        }
    }
}
