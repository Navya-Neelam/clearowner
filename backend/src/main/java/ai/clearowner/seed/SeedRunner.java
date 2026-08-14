package ai.clearowner.seed;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Loads the synthetic dataset into CognoDB.
 * <p>
 * Runs only when the application is started with --seed, so the same jar serves
 * both the API and the data load without a second build or a second language.
 * <pre>
 *   java -jar backend.jar --seed
 * </pre>
 * Every write goes through UNWIND over a parameter list. Nothing is interpolated
 * into a query string.
 */
@Component
public class SeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedRunner.class);
    private static final int BATCH = 250;

    private final Driver driver;
    private final SeedDataFactory factory;
    private final ConfigurableApplicationContext context;

    public SeedRunner(Driver driver, SeedDataFactory factory, ConfigurableApplicationContext context) {
        this.driver = driver;
        this.factory = factory;
        this.context = context;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!args.containsOption("seed") && !args.getNonOptionArgs().contains("seed")) {
            return;
        }

        long start = System.currentTimeMillis();
        log.info("Generating synthetic ownership graph...");
        SeedData data = factory.generate();
        log.info("Generated {} jurisdictions, {} addresses, {} people, {} companies, {} ownerships, {} directorships",
                data.jurisdictions().size(), data.addresses().size(), data.people().size(),
                data.companies().size(), data.ownerships().size(), data.directorships().size());

        boolean force = args.containsOption("force") || args.getNonOptionArgs().contains("force");

        try (Session session = driver.session()) {
            createConstraints(session);
            wipe(session, force);
            loadJurisdictions(session, data);
            loadAddresses(session, data);
            loadPeople(session, data);
            loadCompanies(session, data);
            loadOwnerships(session, data);
            loadDirectorships(session, data);
            report(session);
        } catch (Exception e) {
            log.error("Seeding failed: {}", e.getMessage(), e);
            System.exit(SpringApplication.exit(context, () -> 1));
        }

        log.info("Seed complete in {} ms", System.currentTimeMillis() - start);
        System.exit(SpringApplication.exit(context, () -> 0));
    }

    private void createConstraints(Session session) {
        List<String> statements = List.of(
                "CREATE CONSTRAINT company_id IF NOT EXISTS FOR (c:Company) REQUIRE c.companyId IS UNIQUE",
                "CREATE CONSTRAINT person_id IF NOT EXISTS FOR (p:Person) REQUIRE p.personId IS UNIQUE",
                "CREATE CONSTRAINT jurisdiction_code IF NOT EXISTS FOR (j:Jurisdiction) REQUIRE j.code IS UNIQUE",
                "CREATE CONSTRAINT address_id IF NOT EXISTS FOR (a:Address) REQUIRE a.addressId IS UNIQUE",
                "CREATE INDEX company_name IF NOT EXISTS FOR (c:Company) ON (c.name)",
                "CREATE INDEX person_name IF NOT EXISTS FOR (p:Person) ON (p.name)");
        for (String statement : statements) {
            session.executeWrite(tx -> tx.run(statement).consume());
        }
        log.info("Constraints and indexes ensured");
    }

    /**
     * Seeding replaces the whole graph, so refuse to delete an existing dataset
     * unless the caller asked for it explicitly with --force.
     */
    private void wipe(Session session, boolean force) {
        long existing = session.executeRead(tx ->
                tx.run("MATCH (n) RETURN count(n) AS total").single().get("total").asLong());

        if (existing > 0 && !force) {
            throw new IllegalStateException(
                    "Database already holds %d nodes. Re-run with --seed --force to replace it."
                            .formatted(existing));
        }
        if (existing > 0) {
            session.executeWrite(tx -> tx.run("MATCH (n) DETACH DELETE n").consume());
            log.info("Cleared {} existing nodes", existing);
        }
    }

    private void loadJurisdictions(Session session, SeedData data) {
        write(session, "jurisdictions", data.jurisdictions(), j -> Map.of(
                        "code", j.code(),
                        "name", j.name(),
                        "riskRating", j.riskRating(),
                        "secrecyHaven", j.secrecyHaven()),
                """
                UNWIND $rows AS row
                MERGE (j:Jurisdiction {code: row.code})
                SET j.name = row.name,
                    j.riskRating = row.riskRating,
                    j.secrecyHaven = row.secrecyHaven
                """);
    }

    private void loadAddresses(Session session, SeedData data) {
        write(session, "addresses", data.addresses(), a -> Map.of(
                        "addressId", a.addressId(),
                        "line", a.line(),
                        "city", a.city(),
                        "postalCode", a.postalCode(),
                        "jurisdictionCode", a.jurisdictionCode()),
                """
                UNWIND $rows AS row
                MERGE (a:Address {addressId: row.addressId})
                SET a.line = row.line,
                    a.city = row.city,
                    a.postalCode = row.postalCode
                WITH a, row
                MATCH (j:Jurisdiction {code: row.jurisdictionCode})
                MERGE (a)-[:LOCATED_IN]->(j)
                """);
    }

    private void loadPeople(Session session, SeedData data) {
        write(session, "people", data.people(), p -> Map.of(
                        "personId", p.personId(),
                        "name", p.name(),
                        "birthYear", p.birthYear(),
                        "nationality", p.nationality(),
                        "pep", p.pep()),
                """
                UNWIND $rows AS row
                MERGE (p:Person {personId: row.personId})
                SET p.name = row.name,
                    p.birthYear = row.birthYear,
                    p.pep = row.pep
                WITH p, row
                MATCH (j:Jurisdiction {code: row.nationality})
                MERGE (p)-[:RESIDENT_OF]->(j)
                """);
    }

    private void loadCompanies(Session session, SeedData data) {
        write(session, "companies", data.companies(), c -> Map.of(
                        "companyId", c.companyId(),
                        "name", c.name(),
                        "status", c.status(),
                        "incorporationDate", c.incorporationDate(),
                        "companyType", c.companyType(),
                        "jurisdictionCode", c.jurisdictionCode(),
                        "addressId", c.addressId()),
                """
                UNWIND $rows AS row
                MERGE (c:Company {companyId: row.companyId})
                SET c.name = row.name,
                    c.status = row.status,
                    c.incorporationDate = row.incorporationDate,
                    c.companyType = row.companyType
                WITH c, row
                MATCH (j:Jurisdiction {code: row.jurisdictionCode})
                MERGE (c)-[:REGISTERED_IN]->(j)
                SET c.jurisdictionCode = j.code,
                    c.jurisdictionName = j.name,
                    c.secrecyHaven = j.secrecyHaven
                WITH c, row
                MATCH (a:Address {addressId: row.addressId})
                MERGE (c)-[:REGISTERED_AT]->(a)
                """);
    }

    /**
     * Split by owner type so each statement matches a fixed label. Choosing the
     * label at runtime would mean building the query as a string, which is exactly
     * what the assignment rules out.
     */
    private void loadOwnerships(Session session, SeedData data) {
        List<SeedData.Ownership> byPerson = data.ownerships().stream()
                .filter(o -> "Person".equals(o.ownerType())).toList();
        List<SeedData.Ownership> byCompany = data.ownerships().stream()
                .filter(o -> "Company".equals(o.ownerType())).toList();

        Function<SeedData.Ownership, Map<String, Object>> mapper = o -> Map.of(
                "ownerId", o.ownerId(),
                "companyId", o.companyId(),
                "percentage", o.percentage(),
                "shareClass", o.shareClass(),
                "since", o.since());

        write(session, "ownerships (person owners)", byPerson, mapper,
                """
                UNWIND $rows AS row
                MATCH (owner:Person {personId: row.ownerId})
                MATCH (target:Company {companyId: row.companyId})
                MERGE (owner)-[r:OWNS]->(target)
                SET r.percentage = row.percentage,
                    r.shareClass = row.shareClass,
                    r.since = row.since
                """);

        write(session, "ownerships (company owners)", byCompany, mapper,
                """
                UNWIND $rows AS row
                MATCH (owner:Company {companyId: row.ownerId})
                MATCH (target:Company {companyId: row.companyId})
                MERGE (owner)-[r:OWNS]->(target)
                SET r.percentage = row.percentage,
                    r.shareClass = row.shareClass,
                    r.since = row.since
                """);
    }

    private void loadDirectorships(Session session, SeedData data) {
        write(session, "directorships", data.directorships(), d -> Map.of(
                        "personId", d.personId(),
                        "companyId", d.companyId(),
                        "role", d.role(),
                        "appointedOn", d.appointedOn(),
                        "active", d.active()),
                """
                UNWIND $rows AS row
                MATCH (p:Person {personId: row.personId})
                MATCH (c:Company {companyId: row.companyId})
                MERGE (p)-[r:DIRECTOR_OF]->(c)
                SET r.role = row.role,
                    r.appointedOn = row.appointedOn,
                    r.active = row.active
                """);
    }

    private <T> void write(Session session, String label, List<T> items,
                           Function<T, Map<String, Object>> mapper, String cypher) {
        int written = 0;
        for (int i = 0; i < items.size(); i += BATCH) {
            List<Map<String, Object>> rows = new ArrayList<>();
            for (T item : items.subList(i, Math.min(i + BATCH, items.size()))) {
                rows.add(new HashMap<>(mapper.apply(item)));
            }
            session.executeWrite(tx -> tx.run(cypher, Map.of("rows", rows)).consume());
            written += rows.size();
        }
        log.info("  loaded {} {}", written, label);
    }

    private void report(Session session) {
        var counts = session.executeRead(tx -> tx.run("""
                MATCH (n)
                WITH labels(n)[0] AS label, count(*) AS c
                RETURN label, c ORDER BY label
                """).list());
        counts.forEach(r -> log.info("  {} nodes: {}", r.get("label").asString(), r.get("c").asLong()));

        var rels = session.executeRead(tx -> tx.run("""
                MATCH ()-[r]->()
                RETURN type(r) AS type, count(*) AS c ORDER BY type
                """).list());
        rels.forEach(r -> log.info("  {} relationships: {}", r.get("type").asString(), r.get("c").asLong()));
    }
}
