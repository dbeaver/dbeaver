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
    private static final Pattern CONNECTION = Pattern.compile("(?i)\\b(?:jdbc:[^\\s,\\\"'<>]+|(?:https?|ssh)://[^\\s,\\\"'<>]+)");
    private static final Pattern URI_WITH_USERINFO = Pattern.compile(
        "(?i)\\b[a-z][a-z0-9+.-]*://[^\\s,\\\"'<>/@]+@[^\\s,\\\"'<>]+"
    );
    private static final Pattern AUTHORIZATION_HEADER = Pattern.compile(
        "(?i)(\\b(?:proxy-)?authorization\\s*:\\s*)[^\\r\\n]+"
    );
    private static final Pattern CREDENTIAL = Pattern.compile(
        "(?i)((['\"]?)(?:password|passwd|pwd|secret|token|api[_-]?key|user(?:name)?|authorization)\\2\\s*[=:]\\s*)"
            + "(\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'|[^\\s,;&}]+)"
    );
    private static final Pattern SQL_PASSWORD = Pattern.compile(
        "(?i)(\\bidentified\\s+by\\s+)(\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'|[^\\s,;]+)"
    );
    private static final Pattern ADDRESS = Pattern.compile(
        "(?i)(?<![\\w.])(?:\\d{1,3}(?:\\.\\d{1,3}){3}|(?=(?:[0-9a-f]{0,4}:){3}|[0-9a-f:]*::)"
            + "(?:[0-9a-f]{0,4}:){2,}[0-9a-f:.]+)(?![\\w.])"
    );
    private static final Pattern HOST = Pattern.compile(
        "(?i)(\\bhost(?:name)?\\s*[=:]\\s*)([\\w.-]+)"
    );
    private static final Pattern LICENSE = Pattern.compile("(?i)(\\bLicense\\s*:\\s*)DB-[\\w-]+");
    private static final Pattern BUNDLE_START_LINE = Pattern.compile(
        "(?m)^(?:\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3} - )?Start \\[[^\\r\\n]*(?:\\r?\\n|$)"
    );
    private static final Pattern DOMAIN = Pattern.compile(
        "(?<![\\w.])(?!\\d+\\.\\d+\\.)(?i:(?:[a-z0-9-]+\\.))+"
            + "(?:[a-z][a-z0-9-]+|[A-Z]{2,})(?![\\w.-])"
    );
    private static final Pattern QUOTED_PATH = Pattern.compile(
        "(?i)([\\\"'])(?:[a-z]:\\\\|/)[^\\r\\n\\\"'<>]+\\1"
    );
    private static final Pattern PATH = Pattern.compile(
        "(?<![\\w:/])(?:[a-zA-Z]:\\\\|/)(?:[^\\s\\\"'<>;,]+[/\\\\])*[^\\s\\\"'<>;,]+"
    );
    private static final Pattern WINDOWS_PATH = Pattern.compile(
        "(?m)(?<![\\w:/])(?:[a-zA-Z]:[/\\\\]|[a-zA-Z]\\\\:\\\\\\\\)[^\\\"'<>;,\\r\\n]+?"
            + "(?=[\\t ]+-{1,2}[a-zA-Z]|[\\t ]*\\r?$|[\\\"'<>;,])"
    );
    private static final Pattern FILE_URI = Pattern.compile(
        "(?im)\\bfile:(?:/+[a-z]:/|[a-z]:/|/)[^\\\"'<>;,\\r\\n]+?"
            + "(?=[\\t ]+-{1,2}[a-zA-Z]|[\\t ]*\\r?$|[\\\"'<>;,])"
    );
    private static final Pattern PREFERENCE = Pattern.compile(
        "(?m)^((?:\\\\!/|/(?:bundle_defaults|configuration|instance|project|user)/|@(?:org|com)\\.)"
            + "(?:\\\\.|[^=\\\\\\r\\n])*=)([^\\r\\n]*)"
    );
    private static final Pattern TECHNICAL_RESOURCE = Pattern.compile(
        "(?:reference:file:(?:plugins/)?[\\w.-]+|org\\.eclipse\\.ui\\.workbench/[\\w.-]+"
            + "|file:org\\.eclipse\\.[\\w.-]+/[\\w.-]+|@config\\.dir/\\.\\./p2)"
    );
    private static final Pattern PLUGIN_LABEL = Pattern.compile(
        "^[\\t ]*[\\w.-]+ \\(\\d+(?:\\.[\\w-]+)+\\) \"[^\"\\r\\n]*\" \\[(?:Resolved|Active|Starting)\\]\\r?$"
    );
    private static final Pattern SECURITY_OID = Pattern.compile(
        "\\b(?:Aliases|Algorithm):[\\t ]+(?:OID\\.)?[012](?:\\.\\d+)+"
    );
    private static final Pattern SECURITY_CLASS = Pattern.compile("\\bClass:[\\t ]+[\\w.$]+");
    private static final Pattern SECURITY_ALIAS = Pattern.compile("\\bAliases:[\\t ]+[A-Z0-9()_-]+/[A-Z0-9_-]+");
    private static final Pattern XML_CLOSING_TAG = Pattern.compile("</[\\w:.-]+>");

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
    private static final Pattern SQL_SERVER_VERSION = Pattern.compile(
        "(?i)\\bMicrosoft SQL Server[^\\r\\n]*?\\b\\d+(?:\\.\\d+){3,}"
    );
    private static final Pattern ECLIPSE_LOG_BUNDLE = Pattern.compile("!(?:ENTRY|SUBENTRY\\s+\\d+)\\s+");
    private static final Pattern TECHNICAL_IDENTIFIER = Pattern.compile(
        "(?i)(?:[a-z][a-z0-9_-]*\\.)+[a-z][a-z0-9_-]*(?:\\.[a-z][a-z0-9_-]*)*"
    );
    private static final Pattern PROPERTY_NAME = Pattern.compile("(?m)^[\\t ]*[\\w.-]+[\\t ]*(?=[=:])");
    private static final Pattern MODULE_ARGUMENT = Pattern.compile(
        "--(?:add-opens|add-exports|add-reads|add-modules|limit-modules)(?:=|[\\t ]+)[\\w.,/=-]+"
    );
    private static final Pattern LAUNCHER_ARGUMENT = Pattern.compile("--[a-zA-Z][\\w.-]*");
    private static final Pattern FRAMEWORK_ARGUMENT = Pattern.compile("-[a-zA-Z][\\w.-]*");
    private static final Pattern TECHNICAL_CONTEXT = Pattern.compile(
        "(?i)\\b(?:logger(?:name)?|datasource(?:id)?|driver(?:id)?|resource|key|id|name)\\s*[=:]\\s*$"
    );
    private static final Pattern JAVA_PACKAGE = Pattern.compile("(?i)(?:org|com|net|io|java|javax|meta)\\..*");
    private static final Pattern BUNDLE_IDENTIFIER = Pattern.compile(
        "\\bbundle:/[\\w.-]+|(?m)^[\\t ]*[\\w.-]+(?= \\(\\d+(?:\\.[\\w-]+)+\\) \")"
            + "|(?m)^[\\t ]*Id:[\\t ]*[\\w.-]+(?=,[\\t ]*Version:)"
    );
    private static final Pattern SYSTEM_METADATA = Pattern.compile(
        "^[\\t ]*org\\.osgi\\.framework\\.system\\.(?:packages(?:\\.extra)?|capabilities(?:\\.extra)?)\\s*="
    );
    private static final Pattern DEBIAN_VERSION = Pattern.compile("\\bDebian[\\t ]+\\d+(?:\\.\\d+)+(?:[-+~][\\w.+~:-]+)*");

    private final List<Pattern> environmentValues;
    private final List<Pattern> secretEnvironmentValues;

    DiagnosticInfoSanitizer() {
        this(System.getenv());
    }

    DiagnosticInfoSanitizer(@NotNull Map<String, String> environment) {
        secretEnvironmentValues = environment.entrySet().stream()
            .filter(entry -> entry.getValue() != null && !entry.getValue().isBlank()
                && SENSITIVE_ENV_KEY.matcher(entry.getKey()).find())
            .sorted(Comparator.comparingInt((Map.Entry<String, String> entry) -> entry.getValue().length()).reversed())
            .map(entry -> Pattern.compile(Pattern.quote(entry.getValue())))
            .toList();
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
        // Eclipse preference keys use slash-separated scopes, not filesystem paths.
        Matcher preferences = PREFERENCE.matcher(text);
        StringBuilder result = new StringBuilder(text.length());
        int offset = 0;
        while (preferences.find()) {
            result.append(sanitizeValues(text.substring(offset, preferences.start())));
            String key = preferences.group(1);
            String value = preferences.group(2);
            result.append(key);
            int nameStart = key.lastIndexOf('/') + 1;
            String name = key.startsWith("@") ? "version=" : key.substring(nameStart);
            if (!PROPERTY_NAME.matcher(name).lookingAt()) {
                name = "preference=";
            }
            // Retain the credential key so the ordinary credential rules also apply to preference values.
            String sanitized = sanitizeValues(name + value);
            result.append(sanitized.substring(name.length()));
            offset = preferences.end();
        }
        result.append(sanitizeValues(text.substring(offset)));
        return result.toString();
    }

    @NotNull
    private String sanitizeValues(@NotNull String text) {
        String result = CONNECTION.matcher(text).replaceAll(m -> Matcher.quoteReplacement(redactLocation(m.group())));
        result = URI_WITH_USERINFO.matcher(result).replaceAll(m -> Matcher.quoteReplacement(redactLocation(m.group())));
        result = AUTHORIZATION_HEADER.matcher(result).replaceAll(m -> Matcher.quoteReplacement(m.group(1) + REDACTED));
        result = SQL_PASSWORD.matcher(result).replaceAll(m -> {
            String value = m.group(2);
            String quote = value.startsWith("\"") || value.startsWith("'") ? value.substring(0, 1) : "";
            return Matcher.quoteReplacement(m.group(1) + quote + REDACTED + quote);
        });
        result = CREDENTIAL.matcher(result).replaceAll(m -> {
            String value = m.group(3);
            String quote = value.startsWith("\"") || value.startsWith("'") ? value.substring(0, 1) : "";
            return Matcher.quoteReplacement(m.group(1) + quote + REDACTED + quote);
        });
        result = HOST.matcher(result).replaceAll(m -> Matcher.quoteReplacement(m.group(1) + REDACTED));
        result = LICENSE.matcher(result).replaceAll(m -> Matcher.quoteReplacement(m.group(1) + REDACTED));
        result = QUOTED_PATH.matcher(result).replaceAll(m -> Matcher.quoteReplacement(m.group(1) + REDACTED + m.group(1)));
        result = FILE_URI.matcher(result).replaceAll(m -> Matcher.quoteReplacement(redactLocation(m.group())));
        result = WINDOWS_PATH.matcher(result).replaceAll(m -> Matcher.quoteReplacement(redactLocation(m.group())));
        String beforePaths = result;
        result = PATH.matcher(result).replaceAll(m ->
            Matcher.quoteReplacement(isStackFrame(beforePaths, m.start())
                || isWithin(TECHNICAL_RESOURCE, beforePaths, m.start(), m.end())
                || isWithin(SECURITY_ALIAS, beforePaths, m.start(), m.end())
                || isWithin(XML_CLOSING_TAG, beforePaths, m.start(), m.end())
                || isWithin(MODULE_ARGUMENT, beforePaths, m.start(), m.end())
                ? m.group() : redactLocation(m.group())));
        String beforeAddresses = result;
        result = ADDRESS.matcher(result).replaceAll(m -> {
            String line = lineAt(beforeAddresses, m.start());
            // The four numeric components of a product version can also look like an IPv4 address.
            boolean productVersion = line.contains("DBeaver") && line.contains(" is starting");
            int offset = m.start() - beforeAddresses.lastIndexOf('\n', m.start() - 1) - 1;
            boolean version = VERSION.matcher(line).results().anyMatch(v -> v.start() <= offset && v.end() >= offset + m.group().length());
            boolean sqlServerVersion = SQL_SERVER_VERSION.matcher(line).results()
                .anyMatch(v -> v.start() <= offset && v.end() >= offset + m.group().length());
            boolean oid = isWithin(SECURITY_OID, beforeAddresses, m.start(), m.end());
            return productVersion || version || sqlServerVersion || oid ? Matcher.quoteReplacement(m.group()) : REDACTED;
        });
        String beforeDomains = result;
        result = DOMAIN.matcher(result).replaceAll(m -> {
            String prefix = beforeDomains.substring(beforeDomains.lastIndexOf('\n', m.start() - 1) + 1, m.start());
            boolean bundle = ECLIPSE_LOG_BUNDLE.matcher(prefix).matches();
            Matcher property = PROPERTY_NAME.matcher(lineAt(beforeDomains, m.start()));
            boolean propertyName = property.lookingAt() && prefix.length() < property.end();
            boolean identifier = isTechnicalIdentifier(beforeDomains, m.start(), m.end());
            // Other dotted identifiers are only safe in a recognized diagnostic context, not as arbitrary hostnames.
            boolean metadata = isWithin(MODULE_ARGUMENT, beforeDomains, m.start(), m.end())
                || isWithin(LAUNCHER_ARGUMENT, beforeDomains, m.start(), m.end())
                || isWithin(FRAMEWORK_ARGUMENT, beforeDomains, m.start(), m.end())
                || isWithin(BUNDLE_IDENTIFIER, beforeDomains, m.start(), m.end())
                || isWithin(TECHNICAL_RESOURCE, beforeDomains, m.start(), m.end())
                || isWithin(SECURITY_CLASS, beforeDomains, m.start(), m.end())
                || isSystemMetadataIdentifier(beforeDomains, m.start(), m.end())
                || isWithin(DEBIAN_VERSION, beforeDomains, m.start(), m.end());
            return isStackFrame(beforeDomains, m.start()) || bundle || propertyName || identifier || metadata
                ? Matcher.quoteReplacement(m.group()) : REDACTED;
        });
        return sanitizeEnvironmentValues(result);
    }

    @NotNull
    private String sanitizeEnvironmentValues(@NotNull String text) {
        String result = text;
        for (Pattern pattern : environmentValues) {
            String before = result;
            result = pattern.matcher(result).replaceAll(m -> Matcher.quoteReplacement(
                isWithin(PROPERTY_NAME, before, m.start(), m.end())
                    || isWithin(TECHNICAL_RESOURCE, before, m.start(), m.end())
                    || isWithin(SECURITY_CLASS, before, m.start(), m.end())
                    || PLUGIN_LABEL.matcher(lineAt(before, m.start())).matches()
                    ? m.group() : REDACTED));
        }
        // Technical metadata exemptions must never preserve a known secret.
        for (Pattern pattern : secretEnvironmentValues) {
            String before = result;
            result = pattern.matcher(result).replaceAll(m -> Matcher.quoteReplacement(
                isWithin(PROPERTY_NAME, before, m.start(), m.end()) ? m.group() : REDACTED));
        }
        return result;
    }

    private static boolean isTechnicalIdentifier(@NotNull String text, int start, int end) {
        String line = lineAt(text, start);
        String prefix = line.substring(0, start - (text.lastIndexOf('\n', start - 1) + 1));
        if (prefix.endsWith("@")) {
            return false;
        }
        String identifier = line.substring(start - (text.lastIndexOf('\n', start - 1) + 1),
            end - (text.lastIndexOf('\n', start - 1) + 1));
        return TECHNICAL_IDENTIFIER.matcher(identifier).matches()
            && (JAVA_PACKAGE.matcher(identifier).matches() || TECHNICAL_CONTEXT.matcher(prefix).find());
    }

    private static boolean isStackFrame(@NotNull String text, int position) {
        return STACK_FRAME.matcher(lineAt(text, position)).matches();
    }

    private static boolean isWithin(@NotNull Pattern pattern, @NotNull String text, int start, int end) {
        Matcher matcher = pattern.matcher(lineAt(text, start));
        int lineStart = text.lastIndexOf('\n', start - 1) + 1;
        while (matcher.find()) {
            if (matcher.start() <= start - lineStart && matcher.end() >= end - lineStart) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSystemMetadataIdentifier(@NotNull String text, int start, int end) {
        String line = lineAt(text, start);
        Matcher property = SYSTEM_METADATA.matcher(line);
        if (!property.lookingAt()) {
            return false;
        }
        int lineStart = text.lastIndexOf('\n', start - 1) + 1;
        start -= lineStart;
        end -= lineStart;
        if (start < property.end()) {
            return false;
        }
        // Only clause namespaces and attribute keys are identifiers. Attribute values may contain private hosts.
        int tokenStart = property.end();
        boolean namespace = true;
        char quote = '\0';
        for (int i = tokenStart; i < start; i++) {
            char c = line.charAt(i);
            if (quote != '\0') {
                if (c == '\\') {
                    i++;
                } else if (c == quote) {
                    quote = '\0';
                }
            } else if (c == '"' || c == '\'') {
                quote = c;
            } else if (c == ',' || c == ';') {
                tokenStart = i + 1;
                namespace = c == ',';
            }
        }
        if (quote != '\0' || !line.substring(tokenStart, start).isBlank()) {
            return false;
        }
        String suffix = line.substring(end).stripLeading();
        return suffix.startsWith("=") || suffix.startsWith(":")
            || namespace && (suffix.isEmpty() || suffix.startsWith(";") || suffix.startsWith(","));
    }

    @NotNull
    private static String redactLocation(@NotNull String value) {
        // Keep enclosing log-message delimiters, but consume balanced delimiters inside URLs (e.g. IPv6 hosts).
        int[] unmatchedClosings = new int[3];
        for (int i = 0; i < value.length(); i++) {
            switch (value.charAt(i)) {
                case '(' -> unmatchedClosings[0]--;
                case ')' -> unmatchedClosings[0]++;
                case '[' -> unmatchedClosings[1]--;
                case ']' -> unmatchedClosings[1]++;
                case '{' -> unmatchedClosings[2]--;
                case '}' -> unmatchedClosings[2]++;
                default -> { }
            }
        }
        int end = value.length();
        int punctuationEnd = end;
        while (punctuationEnd > 0 && value.charAt(punctuationEnd - 1) == ',') {
            punctuationEnd--;
        }
        if (punctuationEnd < end && (punctuationEnd == 0 || delimiter(value.charAt(punctuationEnd - 1)) < 0)) {
            punctuationEnd = end;
        }
        end = punctuationEnd;
        while (end > 0) {
            int delimiter = delimiter(value.charAt(end - 1));
            if (delimiter < 0 || unmatchedClosings[delimiter] <= 0) {
                break;
            }
            unmatchedClosings[delimiter]--;
            end--;
        }
        return REDACTED + value.substring(end);
    }

    private static int delimiter(char value) {
        return switch (value) {
            case ')' -> 0;
            case ']' -> 1;
            case '}' -> 2;
            default -> -1;
        };
    }

    @NotNull
    private static String lineAt(@NotNull String text, int position) {
        int start = text.lastIndexOf('\n', position - 1) + 1;
        int end = text.indexOf('\n', position);
        return text.substring(start, end < 0 ? text.length() : end);
    }
}
