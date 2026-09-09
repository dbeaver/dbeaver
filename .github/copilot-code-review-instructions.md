# Code Review

## Security Review

Review every pull request for security issues in the changed code and its direct
call paths. Report only actionable findings introduced by the pull request,
including the affected file and line, attack scenario, and remediation.

Pay particular attention to:

- SQL, shell, file-path, URL, XML, and deserialization inputs that may be
  controlled by users, database contents, configuration, or remote services.
- Authentication, authorization, permission checks, and unsafe default access.
- Secret, token, password, private key, connection string, or personal-data
  exposure in code, tests, configuration, logs, exceptions, or documentation.
- Unsafe handling of credentials and database connection settings.
- Missing validation, encoding, escaping, canonicalization, or resource limits.
- Insecure temporary files, archive extraction, redirects, TLS configuration,
  and external-process execution.

Do not report speculative concerns without a realistic attack path. Do not
request broad refactoring when a focused fix is sufficient.

## Interface Text

Review changed user-visible interface text for punctuation and tone. Apply
these rules to titles, headings, labels, controls, commands, messages,
descriptions, and helper text. Do not apply them to technical documentation or
release notes unless the text is presented in the interface.

### Ellipses

Use the single ellipsis character `…`, not three periods, at the end of a
command label only when the user must enter information, make a choice, or
confirm an action before it takes effect. Do not use an ellipsis when opening a
window or page completes the command, even if the user can make optional
changes there.

For example, use `Export Data…` and `New Database Connection…`. A command can
have different labels depending on its flow: use `Delete…` when the user must
confirm the deletion, and use `Delete` when the deletion takes effect
immediately. Do not use an ellipsis for `Preferences`, `Properties`, or `View
Details` because opening those pages completes the command.

### Periods

Do not use periods in titles, headings, labels, controls, commands, or other
text that names an interface element or action. In messages, descriptions, and
other explanatory prose, use normal sentence punctuation: end complete
sentences with a period, including standalone messages, and do not add a
period to a fragment.

### Colons

Use a colon after a label that introduces an input field or a group of radio
buttons or checkboxes. Do not use a colon when the label and the text inside
the field form a single phrase.

### Contractions

Do not use contractions in interface text. Write words in full, such as `The
connection cannot be established` rather than `The connection can't be
established`. Contractions are acceptable in longer reading-oriented content,
such as release notes.

### Question Marks

Use a question mark only in a confirmation alert that asks a direct question.
Do not use questions in commands, links, labels, helper text, or informational
messages; rewrite them as commands or statements.

### Exclamation Points

Do not use exclamation points in interface text.

Report only clear violations introduced by the pull request and suggest the
correct user-visible wording.
