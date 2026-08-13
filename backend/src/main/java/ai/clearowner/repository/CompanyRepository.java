package ai.clearowner.repository;

import ai.clearowner.dto.BeneficialOwner;
import ai.clearowner.dto.CompanyDetail;
import ai.clearowner.dto.DirectOwner;
import ai.clearowner.dto.GraphView;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.Path;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class CompanyRepository {

    private final Driver driver;

    public CompanyRepository(Driver driver) {
        this.driver = driver;
    }

    public Optional<CompanyDetail> findById(String companyId) {
        String cypher = """
                MATCH (c:Company {companyId: $companyId})
                OPTIONAL MATCH (c)-[:REGISTERED_AT]->(a:Address)
                RETURN c.companyId              AS companyId,
                       c.name                   AS name,
                       c.status                 AS status,
                       c.companyType            AS companyType,
                       c.incorporationDate      AS incorporationDate,
                       c.jurisdictionCode       AS jurisdictionCode,
                       c.jurisdictionName       AS jurisdictionName,
                       c.secrecyHaven           AS secrecyHaven,
                       a.line                   AS addressLine,
                       a.city                   AS addressCity,
                       COUNT { (c)<-[:OWNS]-() }      AS directOwnerCount,
                       COUNT { (c)-[:OWNS]->() }      AS subsidiaryCount,
                       COUNT { (c)<-[:DIRECTOR_OF]-() } AS directorCount
                """;

        try (var session = driver.session()) {
            return session.executeRead(tx -> tx.run(cypher, Map.of("companyId", companyId))
                    .list(r -> new CompanyDetail(
                            r.get("companyId").asString(),
                            r.get("name").asString(),
                            r.get("status").asString(),
                            r.get("companyType").asString(),
                            r.get("incorporationDate").asString(),
                            r.get("jurisdictionCode").asString(""),
                            r.get("jurisdictionName").asString(""),
                            r.get("secrecyHaven").asBoolean(false),
                            r.get("addressLine").asString(""),
                            r.get("addressCity").asString(""),
                            r.get("directOwnerCount").asInt(),
                            r.get("subsidiaryCount").asInt(),
                            r.get("directorCount").asInt())))
                    .stream().findFirst();
        }
    }

    /** Shareholders exactly as the register records them - a single hop, no inference. */
    public List<DirectOwner> directOwners(String companyId, int limit) {
        String cypher = """
                MATCH (owner)-[r:OWNS]->(c:Company {companyId: $companyId})
                RETURN labels(owner)[0]                          AS type,
                       coalesce(owner.companyId, owner.personId) AS id,
                       owner.name                                AS name,
                       r.percentage                              AS percentage,
                       r.shareClass                              AS shareClass,
                       r.since                                   AS since
                ORDER BY r.percentage DESC
                LIMIT $limit
                """;

        try (var session = driver.session()) {
            return session.executeRead(tx -> tx.run(cypher,
                            Map.of("companyId", companyId, "limit", Values.value(limit)))
                    .list(r -> new DirectOwner(
                            r.get("type").asString(),
                            r.get("id").asString(),
                            r.get("name").asString(),
                            Cypher.round2(r.get("percentage").asDouble(0)),
                            r.get("shareClass").asString(""),
                            r.get("since").asString(""))));
        }
    }

    /**
     * The flagship query. For every path from a natural person down to this
     * company, multiply the percentage on each edge to get that path's share,
     * then sum the shares across all paths - a person reaching the company by two
     * routes holds the total of both.
     * <p>
     * The upper bound of the variable-length pattern must be a literal (Cypher
     * does not accept a parameter there), so it is pinned at a safe ceiling and
     * the caller's depth is enforced with a parameterised predicate on the path
     * length. Nothing is concatenated into the query.
     */
    public List<BeneficialOwner> beneficialOwners(String companyId, int maxDepth,
                                                  double threshold, int limit) {
        String cypher = """
                MATCH path = (p:Person)-[:OWNS*1..8]->(c:Company {companyId: $companyId})
                WHERE length(path) <= $maxDepth
                WITH p,
                     reduce(share = 1.0, r IN relationships(path) | share * r.percentage / 100.0) AS pathShare,
                     length(path) AS hops
                WITH p,
                     sum(pathShare) * 100.0 AS effectivePercentage,
                     count(*)               AS routes,
                     min(hops)              AS shortestPathLength
                WHERE effectivePercentage >= $threshold
                RETURN p.personId AS personId,
                       p.name     AS name,
                       p.pep      AS pep,
                       effectivePercentage,
                       routes,
                       shortestPathLength
                ORDER BY effectivePercentage DESC
                LIMIT $limit
                """;

        try (var session = driver.session()) {
            return session.executeRead(tx -> tx.run(cypher, Map.of(
                            "companyId", companyId,
                            "maxDepth", Values.value(maxDepth),
                            "threshold", Values.value(threshold),
                            "limit", Values.value(limit)))
                    .list(r -> new BeneficialOwner(
                            r.get("personId").asString(),
                            r.get("name").asString(),
                            r.get("pep").asBoolean(false),
                            Cypher.round2(r.get("effectivePercentage").asDouble()),
                            r.get("routes").asInt(),
                            r.get("shortestPathLength").asInt())));
        }
    }

    /**
     * A bounded neighbourhood for the visualiser: everything above the company
     * (its owners, and their owners) and one layer below it.
     */
    public GraphView subgraph(String companyId, int depth, int limit) {
        String upstream = """
                MATCH path = (owner)-[:OWNS*1..6]->(c:Company {companyId: $companyId})
                WHERE length(path) <= $depth
                RETURN path LIMIT $limit
                """;
        String downstream = """
                MATCH path = (c:Company {companyId: $companyId})-[:OWNS*1..2]->(sub:Company)
                RETURN path LIMIT $limit
                """;

        Map<String, GraphView.Node> nodes = new LinkedHashMap<>();
        Map<String, GraphView.Edge> edges = new LinkedHashMap<>();

        try (var session = driver.session()) {
            session.executeRead(tx -> {
                collectPaths(tx.run(upstream, Map.of(
                        "companyId", companyId,
                        "depth", Values.value(depth),
                        "limit", Values.value(limit))).list(), companyId, nodes, edges);
                collectPaths(tx.run(downstream, Map.of(
                        "companyId", companyId,
                        "limit", Values.value(limit))).list(), companyId, nodes, edges);
                return null;
            });
        }

        // The focus company may have no ownership links at all; make sure it still renders.
        if (!nodes.containsKey(companyId)) {
            findById(companyId).ifPresent(c -> nodes.put(companyId,
                    new GraphView.Node(companyId, c.name(), "Company",
                            c.jurisdictionName(), c.secrecyHaven(), true)));
        }
        return new GraphView(new ArrayList<>(nodes.values()), new ArrayList<>(edges.values()));
    }

    private void collectPaths(List<org.neo4j.driver.Record> records, String focusId,
                              Map<String, GraphView.Node> nodes,
                              Map<String, GraphView.Edge> edges) {
        for (var record : records) {
            Path path = record.get("path").asPath();

            // The driver identifies path endpoints by elementId, but the API speaks in
            // business keys, so map one to the other as the nodes are collected.
            Map<String, String> elementIdToKey = new LinkedHashMap<>();

            for (var node : path.nodes()) {
                String id = Cypher.identityOf(node);
                elementIdToKey.put(node.elementId(), id);
                nodes.putIfAbsent(id, new GraphView.Node(
                        id,
                        Cypher.nameOf(node),
                        Cypher.label(node),
                        subtitleOf(node),
                        node.containsKey("secrecyHaven") && node.get("secrecyHaven").asBoolean(false),
                        id.equals(focusId)));
            }

            for (var rel : path.relationships()) {
                String source = elementIdToKey.get(rel.startNodeElementId());
                String target = elementIdToKey.get(rel.endNodeElementId());
                if (source == null || target == null) continue;
                String key = source + "->" + target + ":" + rel.type();
                edges.putIfAbsent(key, new GraphView.Edge(source, target, rel.type(),
                        rel.containsKey("percentage")
                                ? Cypher.round2(rel.get("percentage").asDouble()) : null));
            }
        }
    }

    private String subtitleOf(org.neo4j.driver.types.Node node) {
        if (node.containsKey("jurisdictionName")) {
            return node.get("jurisdictionName").asString("");
        }
        if (node.containsKey("pep") && node.get("pep").asBoolean(false)) {
            return "Politically exposed";
        }
        return "";
    }

    public int longestOwnershipChain(String companyId) {
        String cypher = """
                MATCH path = (p:Person)-[:OWNS*1..6]->(c:Company {companyId: $companyId})
                RETURN max(length(path)) AS longest
                """;
        try (var session = driver.session()) {
            return session.executeRead(tx -> {
                var result = tx.run(cypher, Map.of("companyId", companyId)).list();
                return result.isEmpty() ? 0 : result.get(0).get("longest").asInt(0);
            });
        }
    }

    public boolean partOfCircularStructure(String companyId) {
        String cypher = """
                MATCH (a:Company {companyId: $companyId})-[:OWNS]->(b:Company)-[:OWNS]->(z:Company)
                WHERE z.companyId = a.companyId
                RETURN count(*) AS hits
                UNION
                MATCH (a:Company {companyId: $companyId})-[:OWNS]->(b:Company)-[:OWNS]->(c:Company)-[:OWNS]->(z:Company)
                WHERE z.companyId = a.companyId
                RETURN count(*) AS hits
                """;
        try (var session = driver.session()) {
            return session.executeRead(tx -> tx.run(cypher, Map.of("companyId", companyId))
                    .list().stream().anyMatch(r -> r.get("hits").asInt(0) > 0));
        }
    }

    public boolean ownershipRoutesThroughSecrecyHaven(String companyId) {
        String cypher = """
                MATCH path = (p:Person)-[:OWNS*2..6]->(c:Company {companyId: $companyId})
                WHERE any(x IN nodes(path) WHERE x.secrecyHaven = true)
                RETURN count(*) AS hits LIMIT 1
                """;
        try (var session = driver.session()) {
            return session.executeRead(tx -> {
                var rows = tx.run(cypher, Map.of("companyId", companyId)).list();
                return !rows.isEmpty() && rows.get(0).get("hits").asInt(0) > 0;
            });
        }
    }

    public int companiesAtSameAddress(String companyId) {
        String cypher = """
                MATCH (c:Company {companyId: $companyId})-[:REGISTERED_AT]->(a:Address)
                RETURN COUNT { (a)<-[:REGISTERED_AT]-(:Company) } AS total
                """;
        try (var session = driver.session()) {
            return session.executeRead(tx -> {
                var rows = tx.run(cypher, Map.of("companyId", companyId)).list();
                return rows.isEmpty() ? 0 : rows.get(0).get("total").asInt(0);
            });
        }
    }

    public List<ai.clearowner.dto.Directorship> directors(String companyId, int limit) {
        String cypher = """
                MATCH (p:Person)-[r:DIRECTOR_OF]->(c:Company {companyId: $companyId})
                RETURN p.personId AS personId, p.name AS personName,
                       c.companyId AS companyId, c.name AS companyName,
                       r.role AS role, r.appointedOn AS appointedOn, r.active AS active
                ORDER BY r.active DESC, p.name
                LIMIT $limit
                """;
        try (var session = driver.session()) {
            return session.executeRead(tx -> tx.run(cypher,
                            Map.of("companyId", companyId, "limit", Values.value(limit)))
                    .list(r -> new ai.clearowner.dto.Directorship(
                            r.get("personId").asString(),
                            r.get("personName").asString(),
                            r.get("companyId").asString(),
                            r.get("companyName").asString(),
                            r.get("role").asString(""),
                            r.get("appointedOn").asString(""),
                            r.get("active").asBoolean(false))));
        }
    }

    public List<ai.clearowner.dto.SearchResult> search(String query, int limit) {
        String cypher = """
                MATCH (c:Company)
                WHERE toLower(c.name) CONTAINS toLower($query)
                RETURN 'Company' AS type, c.companyId AS id, c.name AS name,
                       coalesce(c.jurisdictionName, '') AS subtitle
                ORDER BY c.name
                LIMIT $limit
                """;
        try (var session = driver.session()) {
            return session.executeRead(tx -> tx.run(cypher,
                            Map.of("query", query, "limit", Values.value(limit)))
                    .list(r -> new ai.clearowner.dto.SearchResult(
                            r.get("type").asString(),
                            r.get("id").asString(),
                            r.get("name").asString(),
                            r.get("subtitle").asString(""))));
        }
    }
}
