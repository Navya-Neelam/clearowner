# Interview preparation — ClearOwner

Notes for defending this submission. Everything here is grounded in code that is
actually in the repository; nothing is aspirational.

If you only memorise one thing, memorise **the flagship query and why the depth
bound is a literal** (§3.1 and §3.2). That is where a graph-database company will
push hardest.

---

## 1. The thirty-second summary

> Company registers list a company's shareholders one layer deep. When those
> shareholders are themselves companies, the individual actually in control is
> invisible. ClearOwner follows the ownership chain to the end and computes each
> person's effective stake by multiplying the percentage on every edge along a
> path and summing across all paths that reach them.

Then show **Zephyr Capital Ltd**: four corporate shareholders on the register, no
humans; three individuals actually above the 25% control threshold, one of them
reached through three separate paths four layers down.

---

## 2. Graph modelling

### "Why a graph database instead of PostgreSQL?"

Do **not** say "because it's impossible in SQL." It isn't, and the README shows the
working recursive CTE. Saying otherwise gets you caught in the first minute.

Say this instead:

> It is expressible in SQL with a recursive CTE — the README includes one. The
> argument is fit, not possibility. Four reasons: the Cypher is the shape of the
> question rather than recursion scaffolding; traversal follows stored
> relationships instead of re-probing an index at every level; `percentage` is a
> property of the *link*, which is exactly where the model puts it; and the
> traversal will not revisit a node, so circular ownership does not need a manual
> visited-set.

### "Why is Address a node instead of a column?"

> Because the question "which other companies are registered here?" is a
> traversal, not a filter. As a text column it is a string match on free text. As
> a node it is `(Company)-[:REGISTERED_AT]->(Address)<-[:REGISTERED_AT]-(Company)`.
> Twelve companies share one address in this dataset, and that pattern is a
> well-known shell-company indicator.

This is the strongest modelling answer you have. Lead with it if they ask what you
would defend most confidently.

### "Why is percentage on the relationship?"

> It is not a fact about the owner or about the company — it is a fact about the
> link between them. The same person can hold 10% of one company and 90% of
> another. Putting it on the edge is what makes `reduce()` over
> `relationships(path)` possible at all.

### "What would you change about the model?"

Have a real answer ready. Suggested:

> Ownership has no time dimension. `since` exists on the relationship but nothing
> reads it, so the graph represents one snapshot. A real register needs
> `from`/`to` on `OWNS` and every query gated on an as-of date, because
> "who owned this in 2019" is a legitimate compliance question. I left it out
> deliberately — it would have doubled the query complexity for a demonstration.

### "Which of your queries is weakest?"

Answer honestly — it is in the README:

> `sharedAddresses` is a `GROUP BY ... HAVING count(*) >= n`. A relational
> database would find that trivial. It is there because it is genuinely useful to
> an analyst, not because it demonstrates traversal.

Volunteering this is far stronger than being caught by it.

---

## 3. Cypher — the part to know cold

### 3.1 The flagship query, line by line

```cypher
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
RETURN p.personId, p.name, p.pep, effectivePercentage, routes, shortestPathLength
ORDER BY effectivePercentage DESC
LIMIT $limit
```

| Line | What it does |
|---|---|
| `MATCH path = (p:Person)-[:OWNS*1..8]->(c:Company {...})` | Every path from any person to this company, 1 to 8 hops. Anchored on the company, which is an index lookup via the uniqueness constraint |
| `WHERE length(path) <= $maxDepth` | The caller's depth limit. See §3.2 — this is why it isn't in the pattern |
| `reduce(share = 1.0, r IN relationships(path) | share * r.percentage / 100.0)` | Multiplies the percentage on each edge of **one** path. 60% × 50% = 0.30 |
| First `WITH` | One row per path, carrying the person, that path's share, and its length |
| `sum(pathShare)` | Groups by person and adds the shares of **all** their paths — the diamond case |
| `count(*)` | How many paths contributed. `routes > 1` means the register understates them |
| `min(hops)` | How deep you must dig before this person first appears |
| `WHERE effectivePercentage >= $threshold` | The 25% control threshold |

**The worked example to have ready:** A owns 60% of B, B owns 50% of C, so A
effectively controls 30% of C. If A also owns 40% of D and D owns 25% of C, A's
total is 30% + 10% = 40%, reported as `routes = 2`.

### 3.2 "Why is the depth bound a literal 8 when maxDepth is a parameter?"

This is the sharpest question they can ask about parameterisation. Answer:

> Cypher does not accept a parameter inside a variable-length bound —
> `[:OWNS*1..$maxDepth]` is a syntax error. Interpolating the number into the
> query text would mean building Cypher by string concatenation, which the brief
> explicitly rules out. So the pattern is pinned at a safe ceiling of 8 and the
> caller's depth is applied as a parameterised predicate on `length(path)`. Every
> value is still bound.

**The trade-off, if they push:** the engine expands to 8 hops and then filters, so
a request for depth 2 does more work than a hand-written 2-hop pattern. At this
dataset size it is not measurable. At scale you would keep a small set of
pre-written queries per depth and select between them in Java — still no string
building.

### 3.3 "Why 25%?"

> It is the reporting threshold in the EU Anti-Money Laundering Directives and the
> FATF recommendations — the level at which someone must be disclosed as a
> beneficial owner. Not an arbitrary number. The UI lets an analyst drop to 10% or
> 1% to see the rest of the structure.

### 3.4 "How do you detect circular ownership?"

```cypher
MATCH (a:Company)-[:OWNS]->(b:Company)-[:OWNS]->(c:Company)-[:OWNS]->(z:Company)
WHERE z.companyId = a.companyId
  AND a.companyId < b.companyId AND a.companyId < c.companyId
```

Two things to explain:

> Explicit fixed-length hops rather than `[:OWNS*2..4]`, because on this CognoDB
> version a variable-length pattern will not return to a node it has already
> traversed — `(a)-[:OWNS*2..4]->(a)` never matches. I found that with a spike
> before writing application code.

> The `a.companyId < b.companyId` predicates pick a canonical rotation. Without
> them the same three-company ring is reported three times, once per starting
> member.

**Known limitation to volunteer:** only loops of length 2–4 are detected, because
the patterns are explicit. A five-company ring would be missed.

### 3.5 "What indexes does this use?"

> Uniqueness constraints on `Company.companyId`, `Person.personId`,
> `Jurisdiction.code` and `Address.addressId` — each backs an index, and every
> query anchors on one of them. Range indexes on `Company.name` and `Person.name`
> for search.

**Be honest about the gap:** search uses `toLower(name) CONTAINS`, which **cannot**
use the range index. It is a label scan. Invisible at 300 companies, wrong at
300,000 — that needs a full-text index. It is in the README's limitations.

---

## 4. Backend

### "How are connections managed?"

> One `Driver` bean for the whole application. The driver is thread-safe and owns
> its connection pool, capped at 20 against the free tier's limit of 200. Sessions
> are short-lived and always closed with try-with-resources.

### "Why doesn't it verify connectivity at startup?"

> Deliberate. If the database is unreachable the application must still start, so
> it can report the outage through `/api/health` instead of crash-looping on the
> host. That decision was proved right in production — CognoDB had an outage the
> day before submission and the app stayed up and told users exactly what was
> wrong.

That is a true story and a strong one. Use it.

### "Why the official driver and not Spring Data Neo4j?"

> The interesting part of this project is the Cypher. An OGM would hide it behind
> derived methods and make the traversals harder to write and to explain. Every
> query is visible in the repository layer.

### "How would you add caching?"

> The dashboard aggregates are the obvious candidate — they change only when the
> graph changes and are recomputed on every load. A short-TTL cache on
> `InsightService.summary()` would remove most of the repeated work. I did not add
> it because there is no write path yet, so there is no invalidation story, and
> a cache without invalidation is a bug waiting to happen.

### "What happens if CognoDB goes down?"

> The driver throws `ServiceUnavailableException`. A single
> `@RestControllerAdvice` maps that to **503** with a stable `code`, not a 500 —
> the request was valid, the datastore is not answering. The frontend shows a
> banner and each panel shows an error state with a retry button. Requests retry
> transient failures with widening delays before surfacing anything, because the
> free tier suspends when idle.

---

## 5. Frontend

### "How do loading and error states work?"

> One helper, `toState()`, wraps any request into `{ loading, data, error }`. Every
> template handles loading, error, empty and data because the type forces it,
> rather than because each component remembered to. That is how the requirement is
> satisfied systematically instead of by discipline.

### "Why Cytoscape with a dagre layout and not a force-directed one?"

> Ownership genuinely is a hierarchy — owners above, owned below. A force-directed
> layout turns it into an unreadable cloud. Dagre keeps the direction meaningful,
> which is the whole point of the picture.

### "How would you render 100,000 nodes?"

> I would not. The subgraph endpoint is bounded by depth and capped at 200 rows,
> because past roughly 200 nodes a link diagram stops communicating anything. At
> that scale you switch representation: aggregate by jurisdiction or owner and let
> the user drill in, or move to WebGL rendering with level-of-detail. The honest
> answer is that the limit is a design decision, not a rendering problem.

### "Why is the retry policy selective?"

> It retries connection failures and 502/503/504 — things a retry can fix. It does
> not retry 404 or 400, because repeating them only makes genuine errors slow to
> appear.

---

## 6. The CognoDB spike — your best engineering story

If asked how you de-risk building on unfamiliar technology:

> Before writing any application code I spent about forty-five minutes on a spike
> that ran 28 Cypher constructs against a live instance and **asserted
> known-correct answers**, not just that each query executed. Four behaved
> differently from Neo4j.

| Construct | Behaviour | Fix |
|---|---|---|
| `round(x, 2)` | Only the 1-argument form exists | Round in the Java DTO layer |
| `exists { ... }` | Syntax error | `COUNT { ... } > 0` |
| **`NOT (pattern)`** | **Silently wrong** — inline properties inside a negated pattern ignored | `COUNT { pattern } = 0` |
| Variable-length paths | Will not revisit a node, so `(a)-[*2..8]->(a)` never matches | Explicit fixed-length patterns |

**The point to land:**

> The third one does not throw. It returns confident, plausible, wrong answers. I
> only caught it because the spike checked expected *values* rather than checking
> that the query ran. If I had spiked by looking for exceptions, that bug would
> have shipped.

**The gift:** because a traversal will not revisit a node, the beneficial-ownership
query is inherently safe against circular ownership — no infinite loops, no
visited-set. If they ask how you handled cycles, that is the answer.

---

## 7. Things to volunteer before they find them

Naming your own weaknesses reads as judgment. All of these are in the README.

- **No pagination.** Every list caps at 200 rows with no offset.
- **Search cannot use the index.** `CONTAINS` is a scan; needs a full-text index.
- **Jurisdiction fields are denormalised onto `Company`** — duplicated truth with
  no synchronisation. Safe only because the seeder rebuilds the graph wholesale.
  A predicate testing list membership across path nodes did not evaluate correctly
  on this CognoDB version, which is why the flag exists.
- **Circular detection covers loops of 2–4 only.**
- **`riskSignals` issues six sequential queries** — the slowest endpoint at ~1s.
- **The graph canvas has no text alternative** for screen readers; the tables are
  the accessible path to the same data.
- **No authentication**, because there is no user model and the brief did not ask.

---

## 8. Questions to ask them

Have two or three ready.

- What does your ownership or entity-resolution data actually look like — is it
  registry-sourced, or reconstructed from documents?
- How do you handle the temporal dimension? Ownership as-of-a-date was the
  hardest thing I chose to leave out.
- What made you build CognoDB rather than run Neo4j? (You have genuinely useful
  feedback here — the `NOT (pattern)` behaviour is a real bug you found, with a
  reproduction. Offer it.)

---

## 9. If the hosted demo misbehaves on the day

CognoDB had an ingress outage the evening before submission. If it recurs during
the interview, do not scramble:

> The database is a managed service and it had an outage — you can see the app
> handling it correctly rather than crashing, which is the graceful-degradation
> requirement working. The dataset is fully reproducible: one command rebuilds all
> 970 nodes and 2,501 relationships deterministically in about thirty seconds.

Then walk them through the code instead. The seed script *is* the answer to that
situation, and you can say so.
