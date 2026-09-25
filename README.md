# data-quality-rule-engine

A reusable Java library that validates business-partner master data against a
catalog of data-quality rules, plus an example HTTP host that embeds it.

## Modules

| Module              | What it is                                                     |
|---------------------|----------------------------------------------------------------|
| `:engine`           | The library. Zero runtime dependencies, no third-party types on its public API. |
| `:examples:http-api`| A Spring Boot host demonstrating how to embed the library.     |

## Requirements

- JDK 21 or later on `PATH` (the build targets Java 21 through `options.release`).

## Build

```bash
./gradlew build
```

This compiles both modules, checks formatting with Spotless and runs the tests.

## Design

See [DESIGN.md](DESIGN.md) for the public API, fault isolation, bounded memory
and the trade-offs taken.
