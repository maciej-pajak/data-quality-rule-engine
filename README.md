# data-quality-rule-engine

A reusable Java library that validates business-partner master data against a
catalog of data-quality rules, plus an example HTTP host that embeds it.

## Modules

| Module | Part of the product | What it is |
|---|---|---|
| `:engine` | yes | The library. Zero runtime dependencies, no third-party types on its public API. |
| `:examples:rules-catalog-java` | no | The task's rule catalog, written in Java against the engine's `RuleCatalog` seam. A host supplies its own. |
| `:examples:http-api` | no | A Spring Boot host demonstrating how to embed the library. |

Everything under `:examples` is a demonstration. A consumer depends on
`:engine` alone.

The fixture scenario from the task description runs in
[`FixtureScenarioTest`](examples/rules-catalog-java/src/test/java/pl/pajak/dqr/example/rules/FixtureScenarioTest.java):
the three records and three rules go through the real engine and the real
catalog, and fault isolation is covered there too.

## Requirements

- JDK 21 or later on `PATH` (the build targets Java 21 through `options.release`).

## Build

```bash
./gradlew build
```

This compiles every module, checks formatting with Spotless and runs the tests.

## Tests

```bash
./gradlew test
```

Runs the test suites of every module without the Spotless and packaging steps.
A single module, or a single test class, narrows it down:

```bash
./gradlew :engine:test
./gradlew :examples:rules-catalog-java:test --tests '*FixtureScenarioTest'
```

Each module writes an HTML report to
`<module>/build/reports/tests/test/index.html`.

## Design

See [DESIGN.md](DESIGN.md) for the public API, fault isolation, bounded memory
and the trade-offs taken.
