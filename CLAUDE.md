# Data-Quality Rule Engine

A reusable Java library that validates business-partner master data against a
catalog of data-quality rules. Several hosts embed it: an HTTP API, batch
workers, scheduled jobs.

## Language

Everything in this repository is written in English: code, identifiers,
comments, Javadoc, test names, documentation and commit messages.

## Commits

- Conventional Commits: `<type>(<scope>): <subject>`.
- Types: `feat`, `fix`, `refactor`, `test`, `docs`, `build`, `ci`, `chore`.
- Scope is the module or area: `engine`, `http-api`, `build`, `docs`.
- Subject only: one line, imperative mood, lower case, no trailing period.
- No body, no footers, no trailers (no `Co-Authored-By`), no emoji.
- Rationale belongs in `DESIGN.md`, not in commit messages.
- Commit in decision-sized slices, not per file. The history should read as
  the sequence of design decisions that built the library.

Examples:

```
feat(engine): derive decisions through a declarative value mapping
test(engine): cover the fixture batch from the task description
build: target java 21 through options.release
```

## Build

- Java 21 is the baseline. Every module sets `options.release = 21`, which
  also validates the JDK API surface, so a call added after 21 fails here
  instead of at a consumer's runtime.
- No preview features: they pin consumers to one exact JDK.
- Gradle wrapper only. Never assume a `gradle` on `PATH`.
- `./gradlew build` must pass before every commit. It runs Spotless and tests.
- Dependency versions live in `gradle/libs.versions.toml` and nowhere else.

## Architecture rules

- `:engine` is the product. `:examples:http-api` is a demonstration host and
  is never a dependency of the product.
- No third-party type may appear on the public API of `:engine` — no Jackson,
  no Spring. Hosts adapt their own types to the engine's seams.
- Spring stays inside `:examples`.
- `:engine` keeps its runtime dependencies at zero unless `DESIGN.md` records
  a reason.
- Decision mapping stays data. No if/else chain computing a `Decision` inside
  a rule.
- The engine emits results as they are produced. No public method may require
  or return a fully materialised collection of results.
- A failing rule is isolated: it produces a reported error, never an aborted
  run.

## Scope discipline

A smaller slice finished to a high standard beats broad scaffolding. When a
requirement is ambiguous, pick an interpretation, implement it, and record the
assumption in `DESIGN.md` instead of inventing extra requirements.
