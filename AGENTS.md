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

## Codebase

- Language: Java (21)
- Platform: OSGi / Eclipse Equinox
- UI: Eclipse RCP (SWT + JFace)
- DB connectivity: JDBC or custom implementation (e.g. WMI)
- SQL parsing: JSQLParser, ANTLR4 (LSM module)
- Testing: JUnit 5, Mockito, custom OSGi test runner

### Build System

- Apache Maven + Eclipse Tycho. 
- Each plugin is packaged as `eclipse-plugin`; test plugins as `eclipse-test-plugin`.

### Building

- To perform full product build run `mvn package -f product/aggregate/pom.xml -T1C -Pproduct-dbeaver-ce,product-dbeaver-eclipse-ce`  
- Building a single bundle with `mvn package` in bundle folder usually fails.
- To build other products(s) use different profiles. You can find maven profiles list in file `product/pom.xml`.

### Running tests

- Running tests in a single bundle usually fail because OSGI needs ALL bundles to be included in build OR be installed in .m2.
- To run tests over full repo run `mvn verify -f product/aggregate/pom.xml -T1C -Pproduct-dbeaver-ce,product-dbeaver-eclipse-ce`. This will run tests for desktop dbeaver ce and dbeaver eclipse plugin.

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
- Dependencies between bundles are declared in `Require-Bundle` manifest header, not in `pom.xml`.

### Plugin packaging rules

- Every plugin has a `META-INF/MANIFEST.MF` (bundle metadata) and a `pom.xml` with packaging `eclipse-plugin`.
- `plugin.xml` declares Eclipse extension points and extensions.
- Bundle source code is in the `src` folder specified in `build.properties` (as required by Tycho).

### License header

- For OSS repos every Java file must begin with Apache 2.0 license header (`docs/license_header.txt`). Variable ${current-year} must be set to the current year.
- If any existing Java file is modified then current year must be updated too.

### Annotations

- Use `@NotNull` and `@Nullable` from `org.jkiss.code` on all method parameters and return types where applicable.
- Expose object properties to the UI via `@Property` (from `org.jkiss.dbeaver.model.meta`) on getter methods. Object properties are needed in UI rendering.

### Code style

- Follow code style of the existing code. The most recent code has good code style.
- Leave comments in code for all non-obvious algorithms. Do not comment simple or obvious functions.
- Java package imports must be in alphabetical order. SDK import must be separated with one empty line from others and be in the end imports section.
- After code changes/refactoring imports which no longer needed must be removed.

### Hardcode

- Do not hardcode constants, use constants declared in libraries or existing *Constants classes in dbeaver codebase or create new ones if needed.
- Do not hardcode UI text messages, use NLS *Messages bundles instead. But messages in exceptions should be in English.

### Logging

- Use `org.jkiss.dbeaver.Log`.
- Do not use `System.out` or any other logging system unless directly requested.

### Exception handling

- `DBException` and its subclasses are the standard checked exceptions for database errors.
- Wrap `SQLException` and other exception from libraries in `DBException` when surfacing to upper layers.
- Using unchecked runtime exceptions is allowed only in rare cases (when there are no other options).

### Long-running tasks

- Long-running methods should accept a `DBRProgressMonitor monitor` as the first parameter. 
- Use Jobs (by default extend AbstractJob class) or utils like RuntimeUtils to perform asynchronous tasks.

### NLS / Localizations

- Each plugin that has user-visible strings has a `*Messages.java` + `*Messages.properties` (and locale variants).
- Reference strings as `*Messages.MY_STRING_KEY`.
- `plugin.xml` uses `%key` references to the `plugin.properties` file.
- Whenever adding text constant add English localization at least.

## Common Pitfalls / Known Issues

- UI thread safety: All SWT/UI updates must run on the display thread. Use functions like `UIUtils.asyncExec(Runnable)` if needed.
- Java 21 required: The target platform requires `JavaSE-21`. Do not use preview features.

## Creating unit tests

- Create unit tests for all model (non-UI) functions if possible.
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

## Code review

- Report defects introduced by the PR. Cite the file and line, explain when the defect occurs, and suggest a focused fix.
- Flag unintended changes to public APIs, persisted data, configuration, or existing user workflows.
- For a bug fix, check that a regression test covers the failing case. Flag tests weakened to make the change pass.

### Security

- Review changed code and its direct call paths. Report a security finding only when the PR introduces a realistic attack path. Cite the file and line, identify who can trigger it and the impact, and suggest a focused fix.
- Trace input from users, database contents, configuration, and remote services into SQL, shell commands, file paths, URLs, XML parsing, and deserialization. Check validation, encoding, escaping, canonicalization, and resource limits at the point of use.
- Check authentication, authorization, and default access. For operations accessible from Team Edition, verify that the server enforces the appropriate REALM permission. Verify that requests without the required permissions are rejected.
- Flag unsafe handling of credentials and database connection settings. Check code, tests, configuration, logs, exceptions, and documentation for exposed secrets, tokens, passwords, private keys, connection strings, or personal data. Do not quote sensitive values in the review.
- Check temporary-file permissions, archive extraction paths, redirects, TLS configuration (including certificate verification), and external-process execution when those operations change.

### OSGi and product features

- Keep OSGi model bundles usable headlessly. Flag model code or bundle manifests that depend on UI bundles, including SWT and JFace.
- Verify that each new bundle or plugin reaches its intended DBeaver desktop, CloudBeaver, or Team Edition product through an included feature. EE products can include CE features: if the product already includes the CE feature, check the bundle there once; do not search for a duplicate EE entry.

### Desktop UI

- Flag blocking work on the SWT display thread and widget access from background threads.

### Interface text

Review changed user-visible interface text for punctuation and tone. Apply these rules to titles, headings, labels, controls, commands, messages, descriptions, and helper text. Do not apply them to technical documentation or release notes unless the text is presented in the interface.

#### Ellipses

Use the single ellipsis character `…`, not three periods, at the end of a command label only when the user must enter information, make a choice, or confirm an action before it takes effect. Do not use an ellipsis when opening a window or page completes the command, even if the user can make optional changes there.

For example, use `Export Data…` and `New Database Connection…`. A command can have different labels depending on its flow: use `Delete…` when the user must confirm the deletion, and use `Delete` when the deletion takes effect immediately. Do not use an ellipsis for `Preferences`, `Properties`, or `View Details` because opening those pages completes the command.

#### Periods

Do not use periods in titles, headings, labels, controls, commands, or other text that names an interface element or action. In messages, descriptions, and other explanatory prose, use normal sentence punctuation: end complete sentences with a period, including standalone messages, and do not add a period to a fragment.

#### Colons

Use a colon after a label that introduces an input field or a group of radio buttons or checkboxes. Do not use a colon when the label and the text inside the field form a single phrase.

#### Contractions

Do not use contractions in interface text. Write words in full, such as `The connection cannot be established` rather than `The connection can't be established`. Contractions are acceptable in longer reading-oriented content, such as release notes.

#### Question Marks

Use a question mark only in a confirmation alert that asks a direct question. Do not use questions in commands, links, labels, helper text, or informational messages; rewrite them as commands or statements.

#### Exclamation Points

Do not use exclamation points in interface text.

Report only clear violations introduced by the pull request and suggest the correct user-visible wording.
