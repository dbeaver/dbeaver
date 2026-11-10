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
    private static final Pattern URI_WITH_USERINFO = Pattern.compile(
        "(?i)\\b[a-z][a-z0-9+.-]*://[^\\s\\\"'<>/@]+@[^\\s\\\"'<>]+"
    );
    private static final Pattern AUTHORIZATION_HEADER = Pattern.compile(
        "(?i)(\\b(?:proxy-)?authorization\\s*:\\s*)[^\\r\\n]+"
    );
    private static final Pattern CREDENTIAL = Pattern.compile(
        "(?i)((['\"]?)(?:password|passwd|pwd|secret|token|api[_-]?key|user(?:name)?|authorization)\\2\\s*[=:]\\s*)"
            + "(\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'|[^\\s,;&}]+)"
    );
    private static final Pattern ADDRESS = Pattern.compile(
        "(?i)(?<![\\w.])(?:\\d{1,3}(?:\\.\\d{1,3}){3}|(?=(?:[0-9a-f]{0,4}:){3}|[0-9a-f:]*::)"
            + "(?:[0-9a-f]{0,4}:){2,}[0-9a-f:.]+)(?![\\w.])"
    );
    private static final Pattern HOST = Pattern.compile(
        "(?i)(\\b(?:host(?:name)?|server)\\s*[=:]\\s*)([\\w.-]+)"
    );
    private static final Pattern BUNDLE_START_LINE = Pattern.compile(
        "(?m)^(?:\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3} - )?Start \\[[^\\r\\n]*(?:\\r?\\n|$)"
    );
    private static final Pattern DOMAIN = Pattern.compile(
        "(?<![\\w.])(?!\\d+\\.\\d+\\.)(?i:(?:[a-z0-9-]+\\.))+"
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
    private static final Pattern ECLIPSE_LOG_BUNDLE = Pattern.compile("!(?:ENTRY|SUBENTRY\\s+\\d+)\\s+");
    private static final Pattern TECHNICAL_IDENTIFIER = Pattern.compile("(?:org\\.(?:eclipse|jkiss)|com\\.dbeaver)\\..+");
    private static final Pattern PROPERTY_NAME = Pattern.compile("(?m)^[\\t ]*[\\w.-]+[\\t ]*(?=[=:])");

    private final List<Pattern> environmentValues;

    DiagnosticInfoSanitizer() {
        this(System.getenv());
    }

    DiagnosticInfoSanitizer(@NotNull Map<String, String> environment) {
        // Longer values first so a short value cannot split a longer secret.
        environmentValues = environment.entrySet().stream()
            // Build numbers, version strings and shell counters are not secrets; replacing them destroys support information.
            .filter(entry -> entry.getValue() != null
                && (!entry.getValue().matches("\\d+(?:\\.\\d+)*(?:\\.[a-zA-Z][\\w-]*)?")
                || SENSITIVE_ENV_KEY.matcher(entry.getKey()).find()))
            .filter(entry -> !entry.getValue().isBlank())
            .sorted(Comparator.comparingInt((Map.Entry<String, String> entry) -> entry.getValue().length()).reversed())
            .map(entry -> {
                String value = entry.getValue();
                // Ordinary values (e.g. SESSIONNAME=Console) must not split Java identifiers.
                boolean secret = value.length() >= 5 && SENSITIVE_ENV_KEY.matcher(entry.getKey()).find();
                return secret ? Pattern.quote(value) : "(?<![\\w$])" + Pattern.quote(value) + "(?![\\w$])";
            })
            .distinct()
            .map(Pattern::compile)
            .toList();
    }

    @NotNull
    String sanitize(@NotNull String text) {
        // Bundle startup lines contain technical metadata, not connection information.
        Matcher bundleStarts = BUNDLE_START_LINE.matcher(text);
        StringBuilder sanitized = new StringBuilder(text.length());
        int offset = 0;
        while (bundleStarts.find()) {
            sanitized.append(sanitizeText(text.substring(offset, bundleStarts.start())));
            sanitized.append(bundleStarts.group());
            offset = bundleStarts.end();
        }
        sanitized.append(sanitizeText(text.substring(offset)));
        return sanitized.toString();
    }

    @NotNull
    private String sanitizeText(@NotNull String text) {
        String result = CONNECTION.matcher(text).replaceAll(REDACTED);
        result = URI_WITH_USERINFO.matcher(result).replaceAll(REDACTED);
        result = AUTHORIZATION_HEADER.matcher(result).replaceAll(m -> Matcher.quoteReplacement(m.group(1) + REDACTED));
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
        result = DOMAIN.matcher(result).replaceAll(m -> {
            String prefix = beforeDomains.substring(beforeDomains.lastIndexOf('\n', m.start() - 1) + 1, m.start());
            boolean bundle = ECLIPSE_LOG_BUNDLE.matcher(prefix).matches();
            Matcher property = PROPERTY_NAME.matcher(lineAt(beforeDomains, m.start()));
            boolean propertyName = property.lookingAt() && prefix.length() < property.end();
            // Known Java namespaces also occur in Eclipse messages outside stack frames and bundle headers.
            boolean identifier = !prefix.endsWith("@") && TECHNICAL_IDENTIFIER.matcher(m.group()).matches();
            return isStackFrame(beforeDomains, m.start()) || bundle || propertyName || identifier
                ? Matcher.quoteReplacement(m.group()) : REDACTED;
        });
        // Environment values can also occur in property names. Only replace them outside the key.
        Matcher properties = PROPERTY_NAME.matcher(result);
        StringBuilder sanitized = new StringBuilder(result.length());
        int offset = 0;
        while (properties.find()) {
            sanitized.append(sanitizeEnvironmentValues(result.substring(offset, properties.start())));
            sanitized.append(properties.group());
            offset = properties.end();
        }
        sanitized.append(sanitizeEnvironmentValues(result.substring(offset)));
        return sanitized.toString();
    }

    @NotNull
    private String sanitizeEnvironmentValues(@NotNull String text) {
        String result = text;
        for (Pattern pattern : environmentValues) {
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
