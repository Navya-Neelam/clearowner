package ai.clearowner.repository;

import org.neo4j.driver.types.Node;

/**
 * Small helpers shared by the repositories.
 * <p>
 * Rounding happens here rather than in Cypher because CognoDB's {@code round()}
 * takes a single argument - there is no two-argument form to round to decimals.
 */
final class Cypher {

    private Cypher() {
    }

    static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    static String label(Node node) {
        var it = node.labels().iterator();
        return it.hasNext() ? it.next() : "Unknown";
    }

    /** Companies key on companyId, people on personId; this picks whichever applies. */
    static String identityOf(Node node) {
        if (node.containsKey("companyId")) return node.get("companyId").asString();
        if (node.containsKey("personId")) return node.get("personId").asString();
        if (node.containsKey("addressId")) return node.get("addressId").asString();
        if (node.containsKey("code")) return node.get("code").asString();
        return String.valueOf(node.elementId());
    }

    static String nameOf(Node node) {
        if (node.containsKey("name")) return node.get("name").asString();
        if (node.containsKey("line")) return node.get("line").asString();
        return identityOf(node);
    }
}
