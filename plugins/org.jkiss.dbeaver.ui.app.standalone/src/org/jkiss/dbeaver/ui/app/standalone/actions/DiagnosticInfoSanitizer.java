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
        "(?i)(\\b(?:password|passwd|pwd|secret|token|api[_-]?key|user(?:name)?)\\b\\s*[=:]\\s*)([^\\s,;&}]+)"
    );
    private static final Pattern ADDRESS = Pattern.compile(
        "(?i)(?<![\\w.])(?:\\d{1,3}(?:\\.\\d{1,3}){3}|(?:[0-9a-f]{0,4}:){2,}[0-9a-f:.]+)(?![\\w.])"
    );
    private static final Pattern HOST = Pattern.compile(
        "(?i)(\\b(?:host(?:name)?|server)\\s*[=:]\\s*)([\\w.-]+)"
    );
    private static final Pattern DOMAIN = Pattern.compile(
        "(?i)(?<![\\w.@])(?:[a-z0-9-]+\\.)+(?:com|net|org|io|dev|local|internal|cloud|gov|edu)(?![\\w.-])"
    );
    private static final Pattern QUOTED_PATH = Pattern.compile(
        "(?i)([\\\"'])(?:[a-z]:\\\\|/)[^\\r\\n\\\"']+\\1"
    );
    private static final Pattern PATH = Pattern.compile(
        "(?<![\\w:/])(?:[a-zA-Z]:\\\\|/)(?:[^\\s\\\"'<>;,]+[/\\\\])*[^\\s\\\"'<>;,]+"
    );

    private final List<String> environmentValues;
    private final List<Pattern> shortEnvironmentValues;

    DiagnosticInfoSanitizer() {
        this(System.getenv());
    }

    DiagnosticInfoSanitizer(@NotNull Map<String, String> environment) {
        // Longer values first so a short value cannot split a longer secret.
        environmentValues = environment.values().stream()
            .filter(value -> value != null && value.length() >= 5 && !value.isBlank())
            .distinct()
            .sorted(Comparator.comparingInt(String::length).reversed())
            .toList();
        shortEnvironmentValues = environment.values().stream()
            .filter(value -> value != null && !value.isBlank() && value.length() < 5)
            .distinct()
            .map(value -> Pattern.compile("(?<![\\w])" + Pattern.quote(value) + "(?![\\w])"))
            .toList();
    }

    @NotNull
    String sanitize(@NotNull String text) {
        String result = CONNECTION.matcher(text).replaceAll(REDACTED);
        result = CREDENTIAL.matcher(result).replaceAll(m -> Matcher.quoteReplacement(m.group(1) + REDACTED));
        result = HOST.matcher(result).replaceAll(m -> Matcher.quoteReplacement(m.group(1) + REDACTED));
        result = QUOTED_PATH.matcher(result).replaceAll(REDACTED);
        result = PATH.matcher(result).replaceAll(REDACTED);
        result = ADDRESS.matcher(result).replaceAll(REDACTED);
        result = DOMAIN.matcher(result).replaceAll(REDACTED);
        for (String value : environmentValues) {
            result = result.replace(value, REDACTED);
        }
        for (Pattern pattern : shortEnvironmentValues) {
            result = pattern.matcher(result).replaceAll(REDACTED);
        }
        return result;
    }
}
