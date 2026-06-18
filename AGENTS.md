# DBeaver – AI Agent Instructions

## What is DBeaver?

DBeaver Community Edition (CE) is a free, open-source, multi-platform database management tool written in **Java**. It supports 100+ database drivers out of the box and is built on **Eclipse RCP** with an **OSGi** plugin architecture. The commercial product shares the same model layer as DBeaver CE and the browser-based [CloudBeaver](https://github.com/dbeaver/cloudbeaver).

---

## Repository Layout

```
dbeaver/
├── plugins/          # OSGi bundles (source code)
│   ├── org.jkiss.dbeaver.model/          # Core API interfaces (no UI, no JDBC)
│   ├── org.jkiss.dbeaver.model.jdbc/     # JDBC base implementations
│   ├── org.jkiss.dbeaver.model.sql/      # SQL model (dialect, LSM parser glue)
│   ├── org.jkiss.dbeaver.model.lsm/      # ANTLR4-based SQL parser
│   ├── org.jkiss.dbeaver.core/           # Desktop RCP application core
│   ├── org.jkiss.dbeaver.registry/       # Driver/connection registry
│   ├── org.jkiss.dbeaver.ext.{db}/       # Per-DB model plugin (no UI deps)
│   ├── org.jkiss.dbeaver.ext.{db}.ui/    # Per-DB UI plugin
│   ├── org.jkiss.dbeaver.ui.*/           # Shared UI components
│   └── org.jkiss.dbeaver.osgi.test.runner/ # OSGi JUnit 5 test harness
├── test/             # OSGi test plugins (eclipse-test-plugin packaging)
│   ├── org.jkiss.dbeaver.ext.{db}.test/
│   └── org.jkiss.dbeaver.model.sql.test/
├── features/         # Eclipse feature descriptors
├── product/          # Product configurations & aggregator POMs
│   └── aggregate/    # Top-level Maven build entry point
├── docs/
│   ├── codestyle/eclipse-formatter-profile.xml
│   ├── license_header.txt
│   └── devel.txt     # Branch/process overview
├── pom.xml           # Root Tycho Maven POM
└── project.deps      # External dependency repo names (e.g. "dbeaver-common")
```

The build depends on a sibling repository called **`dbeaver-common`** (must be checked out at `../dbeaver-common`).

---

## Technology Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Plugin system | OSGi / Eclipse Equinox |
| UI framework | Eclipse RCP (SWT + JFace) |
| Build system | Apache Maven + Eclipse Tycho |
| DB connectivity | JDBC; optional ODBC/NoSQL in EE |
| SQL parsing | JSQLParser, ANTLR4 (LSM module) |
| Testing | JUnit 5, Mockito, custom OSGi test runner |

---

## Build System

DBeaver uses **Eclipse Tycho** (Maven plugin for OSGi). Each plugin is packaged as `eclipse-plugin`; test plugins as `eclipse-test-plugin`.

### Building

```bash
# Full build from the aggregator
mvn package -f product/aggregate/pom.xml -Pproduct-dbeaver-ce,product-dbeaver-eclipse-ce

# Build only a single plugin (fast iteration)
mvn package -f plugins/org.jkiss.dbeaver.ext.mysql/pom.xml
```

> **CI** runs the same command via the reusable workflow in `.github/workflows/push-pr-devel.yml`.

### Plugin packaging rules

- Every plugin has a `META-INF/MANIFEST.MF` (bundle metadata) and a `pom.xml` with `<packaging>eclipse-plugin</packaging>`.
- Dependencies between plugins are declared in `MANIFEST.MF` under `Require-Bundle:`, **not** in `pom.xml`.
- `plugin.xml` declares Eclipse extension points and extensions.
- All source is under `src/` (no `src/main/java`).

---

## Code Conventions

### Package and class naming

| Prefix | Meaning | Example |
|--------|---------|---------|
| `DBP*` | Platform-level capability | `DBPDataSource`, `DBPObject` |
| `DBS*` | Database structure/metadata | `DBSObject`, `DBSTable`, `DBSSchema` |
| `DBC*` | Connectivity (execution context) | `DBCSession`, `DBCException` |
| `DBD*` | Data values/formatting | `DBDValueHandler`, `DBDDataFilter` |
| `DBR*` | Runtime (progress, jobs) | `DBRProgressMonitor`, `DBRRunnableWithProgress` |
| `JDBC*`| JDBC-specific implementations | `JDBCDataSource`, `JDBCSQLDialect` |

All production code lives in the `org.jkiss.dbeaver.*` namespace.

### License header

Every Java file **must** begin with:

```java
/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-<year> DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * ...
 */
```

See `docs/license_header.txt` for the canonical template.

### Annotations

- Use `@NotNull` and `@Nullable` from `org.jkiss.code` on all method parameters and return types where applicable.
- Expose object properties to the UI via `@Property` (from `org.jkiss.dbeaver.model.meta`) on getter methods.
- Mark associations (child collections) with `@Association`.
- Use `@ForTest` on members that exist solely for unit-testing access.

### Logging

```java
private static final Log log = Log.getLog(MyClass.class);
// ...
log.debug("...");
log.warn("...", exception);
log.error("...", exception);
```

`Log` is `org.jkiss.dbeaver.Log`. Do **not** use `System.out/err` or SLF4J directly.

### Exception handling

- `DBException` (and its subclasses like `DBCException`, `DBDatabaseException`) are the standard checked exceptions for database errors.
- Wrap JDBC `SQLException` in `DBException` when surfacing to upper layers.
- Use `DBWorkbench.getPlatform()` to access platform services (not static singletons passed around).

### Progress monitoring

Long-running operations always accept a `DBRProgressMonitor`:

```java
public void doSomething(DBRProgressMonitor monitor) throws DBException {
    monitor.beginTask("Loading...", 100);
    try {
        // work
        monitor.worked(50);
    } finally {
        monitor.done();
    }
}
```

Use `VoidProgressMonitor.INSTANCE` in tests when a real monitor is not needed.

### NLS / Localization

- Each plugin that has user-visible strings has a `*Messages.java` + `*Messages.properties` (and locale variants).
- Reference strings as `Messages.MY_STRING_KEY`.
- `plugin.xml` uses `%key` references to the `plugin.properties` file.

---

## Architecture Patterns

### Model / UI separation

Plugins are split into pure-model (`ext.mysql`) and UI (`ext.mysql.ui`) bundles. Model plugins **must not** import SWT, JFace, or Eclipse workbench packages. This separation allows the model layer to be reused in server-side products (CloudBeaver).

### Extension-point driven design

Features are contributed via Eclipse extension points declared in `plugin.xml`. Key extension points:

| Extension point ID | Purpose |
|-------------------|---------|
| `org.jkiss.dbeaver.dataSourceProvider` | Register a new database driver/provider |
| `org.jkiss.dbeaver.navigator` (via tree config in plugin.xml) | Define the navigator tree structure for a database |
| `org.jkiss.dbeaver.service` | Register a service implementation |
| `org.jkiss.dbeaver.dataFormatter` | Register a data formatter |
| `org.jkiss.dbeaver.dataTypeProvider` | Register value handler for a SQL type |

### Adding a new database driver

> **Note**: For many drivers, updating `plugin.xml` alone is enough — you only need to implement Java classes when the existing JDBC infrastructure does not cover your use case.

1. Create `plugins/org.jkiss.dbeaver.ext.{db}/` with `META-INF/MANIFEST.MF`, `plugin.xml`, and a `pom.xml` (`eclipse-plugin`).
2. Add an optionally-UI sibling `plugins/org.jkiss.dbeaver.ext.{db}.ui/`.
3. Implement `DBPDataSourceProvider<YourDataSource>` → register it in `plugin.xml` under `org.jkiss.dbeaver.dataSourceProvider`.
4. Implement `JDBCDataSource` (from `org.jkiss.dbeaver.model.jdbc`) for JDBC-based drivers.
5. Implement `SQLDialect` (or extend `JDBCSQLDialect`) for SQL syntax specifics.
6. Add the new plugin to `plugins/pom.xml` `<modules>` list.
7. Add a test plugin `test/org.jkiss.dbeaver.ext.{db}.test/` and register it in `test/pom.xml`.

### JDBCUtils and result set reading

The utility class `org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils` (in `org.jkiss.dbeaver.model.jdbc` bundle) contains `safeGet*` helpers for reading from `ResultSet`/`JDBCResultSet` without checked exceptions:

```java
String name = JDBCUtils.safeGetString(dbResult, "table_name");
long oid = JDBCUtils.safeGetLong(dbResult, "oid");
```

---

## Testing

### Test structure

- Test plugins are in the `test/` directory.
- Each test plugin mirrors a production plugin: `test/org.jkiss.dbeaver.ext.postgresql.test/`.
- Tests extend `DBeaverUnitTest` (from `org.jkiss.dbeaver.osgi.test.runner`) or use `@RunWithApplication`/`@RunWithProduct` annotations for integration tests that need a running OSGi container.

### Running tests

Tests are run by Maven Tycho as part of the standard build. There is no separate test-only Maven command; tests execute during `mvn package` (or `mvn verify`) when the `desktop` profile is active (it is active by default when `!headless-platform`).

### Writing tests

```java
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MyFeatureTest extends DBeaverUnitTest {

    @Test
    public void shouldDoSomething() {
        // given
        var query = new SQLQuery(null, "SELECT 1");
        // then
        assertFalse(query.isDropDangerous());
    }
}
```

Use Mockito for mocking. Common mocks: `DBRProgressMonitor`, `DBPDataSourceContainer`, `DBPDataSource`.

---

## Branches and Git Workflow

- **`devel`** — the main development branch; all PRs must target this branch.
- **`master`** — inactive branch; do not use or commit to it.
- **Release branches** — exist for each release; never commit to them directly.
- Pull requests that only fix typos, formatting, or trivial refactoring are generally **not accepted** per the contributor guide.
- **Naming convention**: issues, commit messages, and PR titles should follow the format `org/repo#issueNumber title` (e.g., `dbeaver/dbeaver#12345 Fix NPE in PostgreSQL dialect`).
- **Branch naming**: branches should follow the format `org/project#issueNumber-issueTitle` (e.g., `dbeaver/dbeaver#12345-fix-npe-postgresql`).
- **Linking PRs to issues**: always link a pull request to its corresponding GitHub issue. Use the GitHub UI "Development" link on the PR sidebar when possible; if a direct link is not available, add `Closes org/project#issueNumber` in the PR description (e.g., `Closes dbeaver/dbeaver#12345`).
- **AI-generated PRs**: large pull requests that are entirely AI-generated are strongly discouraged. Keep AI-assisted contributions focused and small, and ensure each change is understood and reviewed by a human contributor.
- **AI tools disclosure**: if AI tools were used to generate code, mention it in the PR description. Example: *This PR was generated with AI (GitHub Copilot)*.

---

## Common Pitfalls / Known Issues

1. **Build requires sibling `dbeaver-common`**: The root `pom.xml` references `../dbeaver-common/pom.xml` as its parent. Clone `dbeaver-common` alongside this repo before building.
2. **No `src/main/java`**: Sources live directly under `src/` (Tycho convention for OSGi plugins). Do not create Maven standard directory layout.
3. **Dependencies in `MANIFEST.MF`, not `pom.xml`**: Adding a dependency means editing `Require-Bundle:` in `META-INF/MANIFEST.MF`. Maven `<dependencies>` are only for Maven-only artifacts resolved via P2 (`pomDependencies=consider`).
4. **UI thread safety**: All SWT/UI updates must run on the display thread. Use `UIUtils.asyncExec(Runnable)` or `UIUtils.syncExec(Runnable)` (from `org.jkiss.dbeaver.ui`).
5. **`@Property` on getters only**: The `@Property` annotation is processed reflectively at runtime; it must be placed on the getter method, not the field.
6. **Java 21 required**: The target platform requires `JavaSE-21`. Do not use preview features.

---

## Key Files Quick Reference

| File | Purpose |
|------|---------|
| `pom.xml` (root) | Tycho build configuration, Java version, target platforms |
| `plugins/pom.xml` | Aggregator listing all plugin modules |
| `test/pom.xml` | Aggregator listing all test modules |
| `product/aggregate/pom.xml` | Top-level build entry point used by CI |
| `plugins/org.jkiss.dbeaver.model/META-INF/MANIFEST.MF` | Core API bundle exports |
| `docs/license_header.txt` | Required license header for Java files |
| `docs/devel.txt` | Brief contributor workflow notes |
| `.github/workflows/push-pr-devel.yml` | CI: build on PR and push to `devel` |

## Code Contribution Guide

For detailed contribution instructions, see the [Code contribution guide](https://github.com/dbeaver/dbeaver/wiki/Contribute-your-code).
