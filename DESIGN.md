# Design

This is a solution for the `Data-Quality Rule Engine` task.

This document records decisions, not contracts: what each type promises is in its
Javadoc, why it is shaped that way is here. I made many assumptions and decisions during the design process, some might not fit the task's intention,
for a real-life task, all of those would have to be reconsidered before implementation with wider requirements.

## Library scope

`engine` module evaluates a catalog of data-quality rules over
business-partner records and emits one result per (record, rule).

I assumed that engine should support different rules' catalogs, "different" in a sense that:
  - can have different implementations,
  - can have different formats,
  - can have different storages.

The consequences of that interpretation:
  - the engine is rule-agnostic,
  - I provided an example of the rules' catalog in `rules-catalog-java` with simple java implementation,
  - for a product I would consider using a dedicated, **declarative** rules engine library, such as Drools, SpEL, custom, or other,
  - record parsing is done outside the engine, to give the engine more flexibility (f.e. consume other formats, such as CSV, XML).
  - the solution focus was on the `engine` module.

## Public API

Entry point: `RuleEngine.process(EngineInput)` returns a `RunSummary`.

Decisions behind that shape:

- **`process` returns only counts.** No public method takes or returns a
  materialised collection of results, so bounded memory is a property of the API
  and not just of the current implementation. Results leave through the sink.
- **One input object, not a parameter list.** `EngineInput` keeps the required
  seams in the factory and the optional settings (batch size, rule filter) in a
  builder, so a new setting does not change the engine's signature.
- **Sealed interfaces, package-private implementations.** Hosts see behaviour,
  not classes; the implementation can change without breaking them, and the
  engine's own `switch` over `ComputationResult` and `DecisionMapping.Resolution`
  stays exhaustive at compile time.
- **Zero runtime dependencies, no third-party type on the API.**
  A library that drags Jackson or Spring into a host constrains the host's own
  versions; adapting a host document to `PartnerRecord` is cheaper than that.
- **No logging framework.** Everything the engine observes is either a result, an
  error or a summary, all of which the host already receives.

### Seams: what the host answers

| Seam                  | The host decides                                |
| --------------------- | ----------------------------------------------- |
| `PartnerRecordSource` | where records come from, a batch at a time      |
| `PartnerRecord`       | how one record exposes its fields               |
| `RuleCatalog`         | which rules catalog to use |
| `RuleResultSink`      | what happens to each result and error as it is produced |
| `RuleFilter`          | which subset of the catalog a run covers        |

### Seams: what a rule catalog implements

| Seam                | The catalog decides                          |
| ------------------- | -------------------------------------------- |
| `RuleCatalog`       | how the rules are loaded                      |
| `Rule`              | identity, metadata, and the two parts below   |
| `ValueLogic`        | what the rule computes from a record          |
| `DecisionMapping`   | which value means which decision              |

`Decision`, `RuleStatus`, `RuleSeverity` and `CountryScope` are the engine's
vocabulary; a catalog uses them, it does not extend them.

### Why the filter is data, not a predicate

`RuleFilter` is a sealed value tree, not a `Predicate<Rule>` the caller writes.
Rule selection is intentionally configurable but not behaviorally extensible. Hosts can compose the filter 
types defined by the library, but cannot provide arbitrary selection logic. Keeping this behavior library-owned 
reduces the public extension surface, protects engine invariants, and makes the library easier to evolve 
without breaking host implementations.


Country scope is deliberately *not* a caller filter: it depends on the record, not on
the run, so it cannot be decided while loading the catalog. The engine matches
it per record instead.

## Fault isolation

- The unit of isolation is one (rule, record) pair. Nothing coarser is contained,
  nothing finer exists.
- Three ways a rule can fail, all ending as a `RuleError` for that pair: the rule
  reports a failure itself (`ComputationResult.ComputationError`), the rule throws,
  or its mapping covers neither the computed value nor a default
  (`DecisionMapping.Resolution.Unmapped`). The last one depends on the record, so
  it is a per-record error rather than a rejected rule.
- An error carries the record, the rule id and label, a message and the cause, so
  it can be located without re-running anything.
- Errors travel the same way as results — to the sink, as they happen — so the
  caller gets both without waiting for the run to end.
- The one failure that does end a run is the sink's own: a sink that cannot accept
  an outcome leaves the run with nowhere to report, so it surfaces as
  `ResultSinkException` instead of being counted. Outcomes already delivered
  stand; no summary is returned.
- A failure of the catalog or of a batch read also ends the run. Both are the
  run's premise, not a per-record event.

Where to look: `RuleEngineImpl.evaluate`, `RulesRun`.

## Bounded memory and scale

At any moment the engine holds the selected rules and one batch of records:
`<rules> + <batch size> + constant`. Not a product of the two — a batch is
evaluated against every rule before the next batch is read.

The batch size is up to the caller (`EngineInput`), and the sink is served as
each outcome is produced, so nothing accumulates except the summary counters,
which are fixed-size (two `EnumMap`s and two scalars).

Rules are the outer loop and records of a batch the inner one. The consequence a
host can observe: within a batch, outcomes reach the sink grouped by rule.

## Rule selection

I read a contradiction in F6: the caller may filter by status, yet the engine is
to run `RELEASED` rules only. Taken literally the second sentence makes the first
useless, so I let the caller filter by status and made `RELEASED` the default when
no status filter is given.

This is the highest-uncertainty assumption in the design. If the literal reading
wins, the fix is small and I would take it: AND `status(RELEASED)` in the engine
unconditionally and expose any deviation as a named opt-in, so "released only"
becomes a contract instead of a default.

Country scope: a rule with no scope is world wide, a world wide rule matches
every record including one that carries no country, and a rule scoped to a
country matches only records of that country. A rule that does not cover a record
produces neither a result nor an error.

## Assumptions

**Records.** Field values are text and records are flat — no nested objects, no
arrays. A typed accessor is the alternative, but then the engine has to publish a
value-type model of its own on its API; one text accessor is what lets a record
carry any set of fields and lets a host adapt its own document instead of
conforming to mine. Nested support fits the same accessor later through
`JSONPath`-like flattened names, e.g. `address[0].city`. What I accept: a rule
comparing numbers or dates parses the text itself, so a malformed value is that
rule's outcome or that rule's error.

**The `country` field.** Matching a country-scoped rule is the one place where the
engine knows a field by name, on a record that is otherwise schemaless. A host
whose schema names it differently answers it under that name. Making the name
configurable is a small step I did not take.

**Decision mapping.** The engine owns the contract — a rule has no way to return a
`Decision` directly, it returns a value and its mapping resolves it — while the
mapping's *content* is per-rule business data the catalog supplies. That is how
F2 is enforced rather than merely followed: there is no if/else path to a
decision available to a rule. Loading such a mapping from a file is illustrated in
`examples/rules-catalog-java`.

**Provenance.** "no vatId", "len 4" and similar notes in the task are hints for
me, not required provenance. A result reports the computed value, how the mapping
resolved it, and the fields the rule read — the engine observes those reads rather
than asking the rule to declare them.

**Catalog loading.** One catalog snapshot per run, so a long run has a stable set
of rules. Caching or sharing a snapshot across runs is a catalog's business, not
the engine's.

**Batching.** F1 states both that the engine reads a batch and that it runs each
rule on each record, so the engine reads in batches and evaluates one by one.
Batching benefits the source, not the evaluation; a source could hide it
entirely, and I kept it because F1 names it.

## The HTTP API example

The simplest honest scenario: online validation of a small, capped batch (100
records), synchronous, one endpoint. It does not exploit the source's batching,
because at this size there is nothing to exploit.

A real run of a million records is a job, not a request. That shape would be:
upload or reference a dataset, start a run, poll its status, read partial
results, export. Fault tolerance inside one long asynchronous run is where the
real complexity sits — restarts, partial progress, at-least-once delivery to the
sink — and none of it belongs in the engine. Streaming records instead of batching
them is an equally valid host shape.

## Required test scenario

`pl.pajak.dqr.example.rules.FixtureScenarioTest` in
`examples/rules-catalog-java`, running the real engine against the real catalog,
including the fault-isolation case.

## Known gaps

- No validation of a catalog: duplicate rule ids and missing required rule
  attributes pass through.
- The `country` field the engine reads is not part of a result's accessed fields.
- `RuleMatcher` resolves composite filters recursively; a deeply nested filter might fail. One possible solution
  is to merge ands and ors children with parents and limit the depth of the filter tree.
- A broken rule produces one error per record — a million of them on a full run.
  The engine cannot tell a record-dependent failure from a broken rule, so the
  next step is an exception contract a rule uses to declare itself unusable, and
  a rule rejected from the run on that basis.

## Trade-offs and what I'd do next

The rejected alternative worth naming: "different catalogs" could have meant only
different *sources*, with the engine owning the rule format and logic convention.
That library would be shaped very differently — the catalog would answer only
"how are rules loaded", not "how do rules work", and `engine` and
`rules-catalog-java` would collapse into one module. It would be a smaller,
stricter product. I chose rule-agnostic because the hosts named in the task
(HTTP API, batch workers, scheduled jobs) suggest one engine serving catalogs it
does not own. For a real product I would also weigh a declarative rule engine
(Drools, SpEL, or a small DSL) instead of rules in Java.

Next, in order:

1. Confirm the requirements and this design, starting with the `RELEASED`
   question.
2. Close the known gaps above.
3. Production readiness: CI, release procedure, semantic versioning, changelog.
4. Revisit parallelism. Rules are self-contained and records independent, so a
   batch parallelises trivially — but only the sink's contract decides whether
   that is safe, so it is a contract change, not an optimisation.
