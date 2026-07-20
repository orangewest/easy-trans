# CODEBUDDY.md

This file provides guidance to CodeBuddy Code when working with code in this repository.

# 注意，要使用简体中文回复用户

## Overview

`easy-trans` is a generic, annotation-driven **data translation** framework for Java. "Translation" here means: given an object with a raw key field (e.g. `sex=1`, `teacherId=2`), automatically populate companion display/derived fields (e.g. `sexName="男"`, `teacherName="老师2"`) by looking up data from a `TransRepository`. It supports dictionary translation, collection/array translation, object filling, wrapper unwrapping, and multi-level nested translation. Core source is ~hundreds of lines with no third-party dependencies.

## Build & Test

Maven multi-module project, **JDK 25** (`maven.compiler.release=25`), groupId `io.github.orangewest`, version `2.0.0`. There is no checkstyle/spotless/lint configured.

```bash
# Build entire project (root)
mvn clean install

# Build a single module
mvn -pl easy-trans-core clean install
mvn -pl easy-trans-spring-start clean install

# Run all tests
mvn test

# Run a single test class (core module)
mvn -pl easy-trans-core test -Dtest=TransServiceTest

# Run a single test method
mvn -pl easy-trans-core test -Dtest=TransServiceTest#trans1
```

Notes:
- `easy-trans-core` tests use JUnit 5 (`junit-jupiter`) and Lombok (test scope only).
- `TransServiceTest` performs `TransService.init()` and registers all repositories/resolvers in a `@BeforeAll` method — this setup is required for the framework to function in tests.

## Module layout

- **`easy-trans-core`** — framework implementation. Pure Java, no external dependencies. Package root: `io.github.orangewest.trans`.
- **`easy-trans-spring-start`** — Spring Boot 4.1.0 auto-configuration that wires the core into a Spring context; provides GraalVM Native Image support via `EasyTransRuntimeHints` (RuntimeHints). Package root: `io.github.orangewest.trans.spring`.
- **`easy-trans-demo`** — sample Spring Boot consumer app used to verify translation end-to-end and as the native-image build target. Package root: `io.github.orangewest.easytrans.demo`.

## Core architecture

### Annotations (package `...trans.annotation`)
- `@Trans` — placed on the **target** field to be filled. `trans()` names the source field/repo key to read from; `key()` selects which field to extract from the looked-up object (defaults to the target field name); `using()` optionally points directly at a `TransRepository` instead of relying on a `@TransRepo`.
- `@TransRepo` — placed on a **source** field (or a custom annotation / class) to bind it to a `TransRepository` via `using()`. Repeatable (container `@TransRepos`). `name()` overrides the repo key (defaults to the field name). A field can be both `@Trans` source and target.
- `@DictTransRepo` — meta-annotation that is itself `@TransRepo(using = DictTransRepository.class)`; adds a `group()` attribute for dictionary translation.

### Extension points (interfaces)
- `TransRepository<T, R>` (`...trans.repository`) — the only interface you implement to add a data source. `getTransValueMap(List<T> values, Annotation anno)` returns `sourceValue -> resultObject`. The whole result object is filled if its type matches the target field; otherwise the `key()` field is extracted.
- `TransObjResolver` (`...trans.resolver`) — unwraps wrapper objects (e.g. `Result<T>`, `PageData<T>`) so translation reaches the inner business object. `support(obj)` + `resolveTransObj(obj)`.

### Static registries
- `TransRepositoryFactory` — `Map<repositoryClass, instance>`, populated by `register(...)`.
- `TransObjResolverFactory` — `List<TransObjResolver>`, populated by `register(...)`.

### Translation engine (package `...trans.service` / `...trans.core`)
- `TransService.trans(obj)` is the entry point. Flow:
  1. `resolveObj` — recursively unwrap via registered `TransObjResolver`s.
  2. Normalize to a `List<Object>`; look up `TransClassMeta` for the class.
  3. `doTrans` — group target fields by their `TransRepoMeta`, and translate each repo group **in parallel** via `CompletableFuture` on a virtual-thread executor (created in `init()`; overridable via `TransService.setExecutor(...)`).
- `TransClassMetaCacheManager` — caches one `TransClassMeta` per class (lazily built on first encounter).
- **Metadata model** (the mental model to read across many files):
  - `TransClassMeta` parses a class into a list of `TransFieldMeta` and a map of `TransRepoMeta`.
  - `TransRepoMeta` = a named source binding: which repo field, which `TransRepository`, whether it's multi-valued (Collection/array), and the source annotation.
  - `TransFieldMeta` = a target field + its `key` + its `TransRepoMeta`, plus `children` for **nested translation**.
  - `TransClassMeta.buildTransTree` links fields whose name equals a repo name into a parent→child tree, enabling multi-level chains (e.g. area → city → province, see `CityDto` in tests).
  - `TransModel` performs the actual reflection read/write (`ReflectUtils`) for one field on one object, handling single value, collections, arrays, and object-vs-field filling.

### Spring integration (package `...trans.spring`)
- `EasyTransAutoConfiguration` registers: `TransService` (calls `init()`), `DictTransRepository` (only if a `DictLoader` bean exists), `EasyTransRegister`, `AutoTransAspect`, `TransUtil`.
- `EasyTransRegister` is a `BeanPostProcessor` that auto-registers every `TransRepository` and `TransObjResolver` Spring bean into the static factories — so in Spring you only annotate your `@Component` repositories/resolvers, no manual `register(...)` calls.
- `AutoTransAspect` intercepts methods annotated `@AutoTrans` (`@Around`) and translates the return value via `TransUtil.transResult`.
- `TransUtil.trans(obj)` is the static entry point usable anywhere in a Spring app.

## Key conventions / gotchas

- Multi-value sources (`List`/`Set`/array fields annotated with `@TransRepo`) drive collection translation; the corresponding `@Trans` target is filled with a list/array of extracted values.
- When a `@Trans` target's type matches the repository result type, the **entire object** is filled (object filling, see `UserDto3.teacher`). Otherwise the `key()` field is extracted.
- The framework intentionally has **no external runtime dependencies** in `easy-trans-core`; keep it that way. Only add dependencies in `easy-trans-spring-start` for Spring concerns.
- Source/target is `maven.compiler.release=25`; modern JDK 25 syntax is acceptable in main source, but keep `easy-trans-core` free of external runtime dependencies.
