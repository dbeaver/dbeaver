---
name: code-review
description: Review pull requests for copy-paste defects and accidentally committed sensitive data. Use this during code review when changed code may have been copied or pasted.
---

# Pull Requests Review

Inspect changed code for signs of copied logic, configuration, tests, and
documentation that were not fully adapted to their new context.

Report a finding only when the copy-paste error can cause incorrect behavior,
security exposure, maintenance risk, or misleading documentation. Check for:

- Stale class, method, variable, plugin, driver, product, or configuration names.
- Logic copied from a related implementation but using the wrong API,
  permission, validation, error handling, resource lifecycle, or UI context.
- New database support copied from an existing database plugin instead of
  extending an applicable generic implementation or base database class. Check
  whether the existing JDBC, SQL dialect, metadata, or other shared framework
  already supports the behavior before duplicating it in a database-specific
  plugin.
- Tests that exercise the original behavior instead of the changed code.
- Duplicated constants, identifiers, URLs, or messages that should differ in
  the new context.
- Pasted secrets, credentials, API keys, tokens, private keys, connection
  strings, personal data, internal URLs, or other sensitive values in source,
  tests, fixtures, configuration, logs, exceptions, and documentation.

For each finding, identify the affected file and line, explain why the pasted
content is incorrect or sensitive in this context, and recommend a focused
fix. Do not report intentional duplication that is already correct and
maintained independently.
