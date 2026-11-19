---
name: java
description: Work with java source code and project build
license: Proprietary
---

## Code:

- Respect existing code style.
- Reuse the JDK, repository code, and `dbeaver-common`; add only generic, tested utilities to `dbeaver-common`.
- Code style configuration for checkstyle located in dbeaver-common/.github/dbeaver-checkstyle-config.xml.
- Always add nullability annotation to new or changed functions and classes.
- When Maven is needed, use the shared `dbeaver-common/mvnw` wrapper on Unix or `dbeaver-common/mvnw.cmd` on Windows.
- Whenever possible use local IDEA MCP Server to search in source code, refactor, debug and trace.

## Unit tests

Antipatterns in unit tests:
- java reflection
- long running tests, tests which run longer than 5 seconds are not good.
- too many mocked entities. use original model wherever it is possible

## Localization

In OSGI-based products (dbeaver, cloudbeaver) we use NLS bundles to keep display messages and their translations. Basically they are properties files with language 2-letter ISO code suffix.
Whenever new display message localization is added to English bundle it has to be translated to all other product languages and corresponding NLS bundles.
We currently support following languages: fr,de,it,ja,ko,pt_BR,ro,ru,zh,es,tw,uk,ar,pl.
