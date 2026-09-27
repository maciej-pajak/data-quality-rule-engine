# Data-Quality Rule Engine

A reusable Java library that validates business-partner master data against a
catalog of data-quality rules. Hosts embed it: an HTTP API, batch workers,
scheduled jobs.

## Modules

- `:engine` — the product. Zero runtime dependencies.
- `:examples:rules-catalog-java` — the task's rules written in Java; an example
  of a catalog source.
- `:examples:http-api` — Spring Boot demonstration host.

Everything under `:examples` demonstrates how to embed the product; none of it
is the product.

## Language

Everything here is English: code, comments, Javadoc, test names, docs, commits.

## Commits

- Conventional Commits: `<type>(<scope>): <subject>`.
- Types: `feat`, `fix`, `refactor`, `test`, `docs`, `build`, `ci`, `chore`.
- Scopes: `engine`, `rules-catalog-java`, `http-api`, `build`, `docs`.
- Subject only: one line, imperative, lower case, no trailing period.
- No body, footers, trailers or emoji. Rationale goes to `DESIGN.md`.
- Commit in decision-sized slices, not per file — the history should read as
  the design decisions that built the library.

```
feat(engine): derive decisions through a declarative value mapping
test(rules-catalog-java): cover the fixture batch from the task description
build: target java 21 through options.release
```

## Build

- Java 21 baseline. Every module sets `options.release = 21`, so an API added
  after 21 fails here instead of at a consumer's runtime. No preview features.
- Compilation is `-Xlint:all -Werror`: warnings are build failures.
- Gradle wrapper only. Never assume a `gradle` on `PATH`.
- `./gradlew build` must pass before every commit (Spotless + tests).
- Dependency versions live in `gradle/libs.versions.toml` and nowhere else.

## Architecture rules

- No third-party type on the public API of `:engine` — no Jackson, no Spring.
  Hosts adapt their own types to the engine's seams.
- Spring stays in `:examples`. Jackson stays `implementation`-scoped.
- `:engine` keeps its runtime dependencies at zero unless `DESIGN.md` records
  a reason. `:examples` is never a product dependency.
- Decision mapping stays data. No if/else chain computing a `Decision` in a
  rule.
- Results are emitted as produced. No public method may require or return a
  fully materialised collection of results.
- A failing rule is isolated: a reported error, never an aborted run.

## Scope discipline

A smaller slice finished well beats broad scaffolding. When a requirement is
ambiguous, pick an interpretation, implement it, and record the assumption in
`DESIGN.md` instead of inventing extra requirements.
