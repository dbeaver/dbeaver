# DBeaver – AI Agent Instructions

## Overview

DBeaver Community Edition (CE) is a free, open-source, multi-platform database management tool written in Java. 
It supports 100+ database drivers (mostly JDBC) and is built on Eclipse RCP with an OSGi plugin architecture.
The commercial products share the same model layer as DBeaver CE + browser-based (GitHub repo `cloudbeaver`) and CLI (GitHub repo `dbvr`).

## Repository Layout

- plugins/bundles: main source code, OSGi bundles
- test: test plugins
- features: Eclipse feature descriptors
- product: Eclipse product configurations + aggregator

## Technology Stack

- Language: Java (21)
- Platform: OSGi / Eclipse Equinox
- UI: Eclipse RCP (SWT + JFace)
- DB connectivity: JDBC or custom implementation (e.g. WMI)
- SQL parsing: JSQLParser, ANTLR4 (LSM module)
- Testing: JUnit 5, Mockito, custom OSGi test runner

## Build System

- Apache Maven + Eclipse Tycho. 
- Each plugin is packaged as `eclipse-plugin`; test plugins as `eclipse-test-plugin`.

### Building

To perform full product build run
`mvn package -f product/aggregate/pom.xml -T 1C -Pproduct-dbeaver-ce,product-dbeaver-eclipse-ce`

To build only a single bundle run `mvn verify` in bundle folder.  
It may fail because of missing dependencies in ~/.m2. In this case run `mvn clean install` once in aggregate product.

### Repo dependencies

- All dbeaver-related repositories are in organization https://github.com/dbeaver
- Each repo may have file `project.deps` in its root. This file is a simple text file, each line contains short name of repository this repository depends on.
- All GitHub repos must be cloned in the same folder (DBEAVER_DEV_HOME - the parent folder of this repository)
- If dep repo is missing on disk AI agent can clone it in DBEAVER_DEV_HOME

### Bundle dependencies

- All OSGI dependencies come from Eclipse P2 repos (not Maven).
- You can find them in root POMs (repos with layout=p2).
- This includes standard Eclipse P2 for RCP development + DBeaver custom P2 (see `repo.p2.eclipse.url`).
- Custom P2 repo source repo is `dbeaver-deps-ce` - it converts classic Maven dependencies into P2 bundles.

### Plugin packaging rules

- Every plugin has a `META-INF/MANIFEST.MF` (bundle metadata) and a `pom.xml` with `<packaging>eclipse-plugin</packaging>`.
- Dependencies between plugins are declared in `MANIFEST.MF` under `Require-Bundle:`, not in `pom.xml`.
- `plugin.xml` declares Eclipse extension points and extensions.
- Bundle source code is in the `src` folder specified in `build.properties` (as required by Tycho).

## Code Conventions

### Package and class naming

- `DBP*` - Platform-level capability (`DBPDataSource`, `DBPObject`)
- `DBS*` - Database structure/metadata (`DBSObject`, `DBSTable`, `DBSSchema`)
- `DBC*` - Connectivity (execution context) (`DBCSession`, `DBCException`)
- `DBD*` - Data values/formatting (`DBDValueHandler`, `DBDDataFilter`)
- `DBR*` - Runtime (progress, jobs) (`DBRProgressMonitor`, `DBRRunnableWithProgress`)

### License header

For OSS repos every Java file must begin with Apache 2.0 license header (it is also in `docs/license_header.txt`)

### Annotations

- Use `@NotNull` and `@Nullable` from `org.jkiss.code` on all method parameters and return types where applicable.
- Expose object properties to the UI via `@Property` (from `org.jkiss.dbeaver.model.meta`) on getter methods.
- Mark associations (child collections) with `@Association`.
- Use `@ForTest` on members that exist solely for unit-testing access.

### Logging

Use `org.jkiss.dbeaver.Log`. Do not use `System.out/err` or SLF4J directly.

### Exception handling

- `DBException` (and its subclasses like `DBCException`, `DBDatabaseException`) are the standard checked exceptions for database errors.
- Wrap JDBC `SQLException` in `DBException` when surfacing to upper layers.
- Using unchecked runtime exceptions is allowed only in exceptional cases (when there are no other options).

### Progress monitoring

- Long-running operations always accept a `DBRProgressMonitor`. 
- Use `new VoidProgressMonitor()` in tests when a real monitor is not needed.

### NLS / Localization

- Each plugin that has user-visible strings has a `*Messages.java` + `*Messages.properties` (and locale variants).
- Reference strings as `*Messages.MY_STRING_KEY`.
- `plugin.xml` uses `%key` references to the `plugin.properties` file.

## Architecture Patterns

### Model / UI separation

Plugins are split into pure-model (e.g. `o.j.d.ext.mysql`, `o.j.d.model.ai`) and UI (`o.j.d.ext.mysql.ui`, `o.j.d.ui.charts`) bundles. 
Model plugins must not depend on SWT, JFace or any other UI-related bundles.

### Extension-point driven design

Features are contributed via Eclipse extension points declared in `plugin.xml`.

### Adding a new database driver

Note: For many drivers, updating `plugin.xml` alone is enough — you only need to implement Java classes when the existing JDBC infrastructure does not cover your use case.

- Create `plugins/org.jkiss.dbeaver.ext.{db}/` with `META-INF/MANIFEST.MF`, `plugin.xml`, and a `pom.xml` (`eclipse-plugin`).
- Add an optionally-UI sibling `plugins/org.jkiss.dbeaver.ext.{db}.ui/`.
- Implement `DBPDataSourceProvider<YourDataSource>` → register it in `plugin.xml` under `org.jkiss.dbeaver.dataSourceProvider`.
- Implement `JDBCDataSource` (from `org.jkiss.dbeaver.model.jdbc`) for JDBC-based drivers.
- Implement `SQLDialect` (or extend `JDBCSQLDialect`) for SQL syntax specifics.
- Add the new plugin to `plugins/pom.xml` `<modules>` list.
- Add a test plugin `test/org.jkiss.dbeaver.ext.{db}.test/` and register it in `test/pom.xml`.

## Testing

- Test plugins are in the `test/` directory.
- Each test plugin mirrors a production plugin: `test/org.jkiss.dbeaver.ext.postgresql.test/`.
- Tests extend `DBeaverUnitTest` (from `org.jkiss.dbeaver.osgi.test.runner`) or use `@RunWithApplication`/`@RunWithProduct` annotations for integration tests that need a running OSGi container.
- Tests are run by Maven Tycho as part of the standard build. 
- There is no separate test-only Maven command; tests execute during `mvn install` or `mvn verify` when the `desktop`.

## Branches and Git Workflow

- `devel` — the main development branch; all PRs must target this branch.
- Release branches — `release_VERSION`, exist for each release; never commit to them directly.
- Pull requests that only fix typos, formatting, or trivial refactoring are generally not accepted per the contributor guide.
- Naming convention: issues, commit messages, and PR titles should follow the format `dbeaver/repo#issueNumber title` (e.g., `dbeaver/dbeaver#12345 Fix NPE in PostgreSQL dialect`).
- Branch naming: branches should follow the format `dbeaver/repo#issueNumber-issueTitle` (e.g., `dbeaver/dbeaver#12345-fix-npe-postgresql`).
- Linking PRs to issues: add `Closes org/project#issueNumber` in the PR description (e.g., `Closes dbeaver/dbeaver#12345`).
- Keep AI-assisted contributions focused and small, and ensure each change is understood and reviewed by a human contributor.
- AI tools disclosure: if AI tools were used to generate code, mention it in the PR description. Example: *This PR was generated with AI (GitHub Copilot)*.

## Common Pitfalls / Known Issues

- UI thread safety: All SWT/UI updates must run on the display thread. Use functions like `UIUtils.asyncExec(Runnable)` if needed.
- `@Property` on getters only: The `@Property` annotation is processed reflectively at runtime; it must be placed on the getter method, not the field.
- Java 21 required: The target platform requires `JavaSE-21`. Do not use preview features.

## Key Files Quick Reference

- `pom.xml` (root) - Tycho build configuration, Java version, target platforms 
- `product/aggregate/pom.xml` - Top-level build entry point used by CI 

## Code Contribution Guide

For detailed contribution instructions, see the [Code contribution guide](https://github.com/dbeaver/dbeaver/wiki/Contribute-your-code).
