/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ui.app.standalone.actions;

import org.jkiss.code.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Sanitizes text only when it is copied into a diagnostic archive. */
final class DiagnosticInfoSanitizer {
    private static final String REDACTED = "[REDACTED]";
    private static final Pattern CONNECTION = Pattern.compile("(?i)\\b(?:jdbc:[^\\s\\\"'<>]+|(?:https?|ssh)://[^\\s\\\"'<>]+)");
    private static final Pattern CREDENTIAL = Pattern.compile(
        "(?i)((['\"]?)(?:password|passwd|pwd|secret|token|api[_-]?key|user(?:name)?)\\2\\s*[=:]\\s*)"
            + "(\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'|[^\\s,;&}]+)"
    );
    private static final Pattern ADDRESS = Pattern.compile(
        "(?i)(?<![\\w.])(?:\\d{1,3}(?:\\.\\d{1,3}){3}|(?=(?:[0-9a-f]{0,4}:){3}|[0-9a-f:]*::)"
            + "(?:[0-9a-f]{0,4}:){2,}[0-9a-f:.]+)(?![\\w.])"
    );
    private static final Pattern HOST = Pattern.compile(
        "(?i)(\\b(?:host(?:name)?|server)\\s*[=:]\\s*)([\\w.-]+)"
    );
    // Bundle identifiers following " - Start [" in log headers are not hostnames.
    private static final Pattern DOMAIN = Pattern.compile(
        "(?<! - Start \\[)(?<![\\w.@])(?!\\d+\\.\\d+\\.)(?i:(?:[a-z0-9-]+\\.))+"
            + "(?:[a-z][a-z0-9-]+|[A-Z]{2,})(?![\\w.-])"
    );
    private static final Pattern QUOTED_PATH = Pattern.compile(
        "(?i)([\\\"'])(?:[a-z]:\\\\|/)[^\\r\\n\\\"']+\\1"
    );
    private static final Pattern PATH = Pattern.compile(
        "(?<![\\w:/])(?:[a-zA-Z]:\\\\|/)(?:[^\\s\\\"'<>;,]+[/\\\\])*[^\\s\\\"'<>;,]+"
    );

    private static final Pattern STACK_FRAME = Pattern.compile(
        "[\\t ]*at[\\t ]+(?:[\\w.]+/)?[\\w.$]+(?:\\.[\\w$<>]+)+"
            + "\\((?:[\\w.$]+\\.java:\\d+|Unknown Source|Native Method)\\)\\r?"
    );
    private static final Pattern SENSITIVE_ENV_KEY = Pattern.compile(
        "(?i)password|passwd|pwd|secret|token|api[_-]?key|auth|credential|private|access[_-]?key"
    );
    private static final Pattern VERSION = Pattern.compile(
        "(?i)\\b\\w*version(?:\\s*[:=]\\s*|\\s+)['\"]?\\d+(?:\\.\\d+){3,}"
    );

    private final List<String> environmentValues;
    private final List<Pattern> shortEnvironmentValues;

    DiagnosticInfoSanitizer() {
        this(System.getenv());
    }

    DiagnosticInfoSanitizer(@NotNull Map<String, String> environment) {
        // Longer values first so a short value cannot split a longer secret.
        List<String> values = environment.entrySet().stream()
            // Build numbers, version strings and shell counters are not secrets; replacing them destroys support information.
            .filter(entry -> entry.getValue() != null
                && (!entry.getValue().matches("\\d+(?:\\.\\d+)*(?:\\.[a-zA-Z][\\w-]*)?")
                || SENSITIVE_ENV_KEY.matcher(entry.getKey()).find()))
            .map(Map.Entry::getValue)
            .toList();
        environmentValues = values.stream()
            .filter(value -> value != null && value.length() >= 5 && !value.isBlank())
            .distinct()
            .sorted(Comparator.comparingInt(String::length).reversed())
            .toList();
        shortEnvironmentValues = values.stream()
            .filter(value -> value != null && !value.isBlank() && value.length() < 5)
            .distinct()
            .map(value -> Pattern.compile("(?<![\\w])" + Pattern.quote(value) + "(?![\\w])"))
            .toList();
    }

    @NotNull
    String sanitize(@NotNull String text) {
        String result = CONNECTION.matcher(text).replaceAll(REDACTED);
        result = CREDENTIAL.matcher(result).replaceAll(m -> {
            String value = m.group(3);
            String quote = value.startsWith("\"") || value.startsWith("'") ? value.substring(0, 1) : "";
            return Matcher.quoteReplacement(m.group(1) + quote + REDACTED + quote);
        });
        result = HOST.matcher(result).replaceAll(m -> Matcher.quoteReplacement(m.group(1) + REDACTED));
        result = QUOTED_PATH.matcher(result).replaceAll(REDACTED);
        String beforePaths = result;
        result = PATH.matcher(result).replaceAll(m ->
            isStackFrame(beforePaths, m.start()) ? Matcher.quoteReplacement(m.group()) : REDACTED);
        String beforeAddresses = result;
        result = ADDRESS.matcher(result).replaceAll(m -> {
            String line = lineAt(beforeAddresses, m.start());
            // The four numeric components of a product version can also look like an IPv4 address.
            boolean productVersion = line.contains("DBeaver") && line.contains(" is starting");
            int offset = m.start() - beforeAddresses.lastIndexOf('\n', m.start() - 1) - 1;
            boolean version = VERSION.matcher(line).results().anyMatch(v -> v.start() <= offset && v.end() >= offset + m.group().length());
            return productVersion || version ? Matcher.quoteReplacement(m.group()) : REDACTED;
        });
        String beforeDomains = result;
        result = DOMAIN.matcher(result).replaceAll(m ->
            isStackFrame(beforeDomains, m.start()) ? Matcher.quoteReplacement(m.group()) : REDACTED);
        for (String value : environmentValues) {
            result = result.replace(value, REDACTED);
        }
        for (Pattern pattern : shortEnvironmentValues) {
            result = pattern.matcher(result).replaceAll(REDACTED);
        }
        return result;
    }

    private static boolean isStackFrame(@NotNull String text, int position) {
        return STACK_FRAME.matcher(lineAt(text, position)).matches();
    }

    @NotNull
    private static String lineAt(@NotNull String text, int position) {
        int start = text.lastIndexOf('\n', position - 1) + 1;
        int end = text.indexOf('\n', position);
        return text.substring(start, end < 0 ? text.length() : end);
    }
}
