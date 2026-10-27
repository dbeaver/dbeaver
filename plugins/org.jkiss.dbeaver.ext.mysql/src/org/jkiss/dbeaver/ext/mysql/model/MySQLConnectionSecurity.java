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
package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.DBCException;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

/**
 * Validates connection inputs without invoking driver parsers, which can execute property transformers.
 * URL options are rejected rather than rewritten: connector versions disagree on precedence and syntax.
 */
final class MySQLConnectionSecurity {
    private static final List<String> BOOLEAN_PROPERTIES = List.of(
        "autoDeserialize", "allowLocalInfile", "allowLoadLocalInfile", "allowUrlInLocalInfile"
    );
    private static final List<String> INDIRECT_PROPERTIES = List.of("propertiesTransform", "useConfigs");
    private static final String INFILE_PATH = "allowLoadLocalInfileInPath";
    private static final int MAX_HOST_NESTING = 16;

    private MySQLConnectionSecurity() {
    }

    static void sanitizeProperties(@NotNull Properties properties) {
        // MariaDB resolves option names case-insensitively, including differently cased duplicates.
        properties.keySet().removeIf(key -> key instanceof String name
            && (INFILE_PATH.equalsIgnoreCase(name) || BOOLEAN_PROPERTIES.stream().anyMatch(p -> p.equalsIgnoreCase(name))));
        BOOLEAN_PROPERTIES.forEach(name -> properties.setProperty(name, "false"));
    }

    static void validate(@Nullable String url, @NotNull Properties properties) throws DBCException {
        for (String name : properties.stringPropertyNames()) {
            validateOption(name, properties.getProperty(name));
        }
        if (url == null) {
            throw unsupportedSyntax();
        }
        int authorityStart = url.indexOf("//");
        if (authorityStart < 0) {
            // MySQL/MariaDB URLs require a literal '//'. Other forms can belong to substituted drivers
            // (for example, a DSN), whose URL grammar is not owned by this policy.
            return;
        }
        authorityStart += 2;
        int queryStart = url.indexOf('?', authorityStart);
        int authorityEnd = queryStart < 0 ? url.length() : queryStart;
        validateAuthorityDelimiter(url, authorityStart, authorityEnd);
        int parentheses = 0;
        int brackets = 0;
        for (int i = authorityStart; i < authorityEnd; i++) {
            char c = url.charAt(i);
            if (c == '/' && parentheses == 0 && brackets == 0) {
                authorityEnd = i;
                break;
            }
            if (c == '(') {
                parentheses++;
            } else if (c == ')') {
                parentheses--;
            } else if (c == '[') {
                brackets++;
            } else if (c == ']') {
                brackets--;
            }
        }
        validateHosts(url.substring(authorityStart, authorityEnd), 0);
        if (queryStart >= 0) {
            // Do not truncate at '#': MariaDB and legacy Connector/J do not treat it as a URI fragment.
            for (String option : url.substring(queryStart + 1).split("&", -1)) {
                validateAssignment(option);
            }
        }
    }

    private static void validateAuthorityDelimiter(@NotNull String url, int start, int end) throws DBCException {
        // Legacy Connector/J searches for the database separator using SQL quote/comment/escape rules.
        // Reject ambiguous authority syntax before truncating it, including comment-adjacent slashes.
        for (int i = start; i < end; i++) {
            char c = url.charAt(i);
            if (c == '\\' || c == '\'' || c == '"' || c == '#') {
                throw unsupportedSyntax();
            }
            if (c == '-' && i + 1 < end && url.charAt(i + 1) == '-'
                && (i + 2 == end || Character.isWhitespace(url.charAt(i + 2))
                    || url.charAt(i + 2) == '\0' || url.charAt(i + 2) == ';')) {
                throw unsupportedSyntax();
            }
            if (c == '/') {
                if ((i + 1 < end && url.charAt(i + 1) == '*') || (i > start && url.charAt(i - 1) == '*')) {
                    throw unsupportedSyntax();
                }
                return;
            }
        }
    }

    private static void validateHosts(@NotNull String hosts, int depth) throws DBCException {
        if (depth > MAX_HOST_NESTING) {
            throw unsupportedSyntax();
        }
        int start = 0;
        int parentheses = 0;
        int brackets = 0;
        for (int i = 0; i < hosts.length(); i++) {
            char c = hosts.charAt(i);
            if (c == '(') {
                parentheses++;
            } else if (c == ')') {
                parentheses--;
            } else if (c == '[') {
                brackets++;
            } else if (c == ']') {
                brackets--;
            } else if (c == ',' && parentheses == 0 && brackets == 0) {
                validateHost(hosts.substring(start, i), depth);
                start = i + 1;
            }
            if (parentheses < 0 || brackets < 0) {
                throw unsupportedSyntax();
            }
        }
        if (parentheses != 0 || brackets != 0) {
            throw unsupportedSyntax();
        }
        validateHost(hosts.substring(start), depth);
    }

    private static void validateHost(@NotNull String host, int depth) throws DBCException {
        host = host.trim();
        int userInfo = host.indexOf('@');
        if (userInfo >= 0) {
            String login = host.substring(0, userInfo);
            if (login.indexOf('(') >= 0 || login.indexOf('[') >= 0
                || host.regionMatches(true, 0, "address=", 0, "address=".length())) {
                throw unsupportedSyntax();
            }
            host = host.substring(userInfo + 1);
        }
        if (host.startsWith("[")) {
            int closing = host.lastIndexOf(']');
            if (closing < 0) {
                throw unsupportedSyntax();
            }
            String content = host.substring(1, closing);
            if (content.indexOf(':') >= 0 && content.indexOf(',') < 0 && content.indexOf('(') < 0) {
                // An IPv6 literal, optionally followed by a port; it cannot contain JDBC assignments.
                validatePlainHost(content.replace(":", "") + host.substring(closing + 1));
            } else if (closing == host.length() - 1) {
                validateHosts(content, depth + 1);
            } else {
                throw unsupportedSyntax();
            }
        } else if (host.regionMatches(true, 0, "address=", 0, "address=".length())) {
            String options = host.substring("address=".length()).trim();
            if (options.isEmpty()) {
                throw unsupportedSyntax();
            }
            while (!options.isEmpty()) {
                int closing = options.indexOf(')');
                if (!options.startsWith("(") || closing < 0) {
                    throw unsupportedSyntax();
                }
                String option = options.substring(1, closing);
                // Legacy Connector/J splits hosts at commas even inside address=(...) values.
                if (option.indexOf(',') >= 0) {
                    throw unsupportedSyntax();
                }
                validateHostAssignment(option);
                options = options.substring(closing + 1).trim();
            }
        } else if (host.startsWith("(") && host.endsWith(")")) {
            for (String option : host.substring(1, host.length() - 1).split(",", -1)) {
                validateHostAssignment(option);
            }
        } else {
            validatePlainHost(host);
        }
    }

    private static void validatePlainHost(@NotNull String host) throws DBCException {
        for (int i = 0; i < host.length(); i++) {
            if ("()[]=,@/?#\"'".indexOf(host.charAt(i)) >= 0) {
                throw unsupportedSyntax();
            }
        }
    }

    private static void validateHostAssignment(@NotNull String option) throws DBCException {
        // Legacy drivers accept malformed and quoted host properties. Only accept the unambiguous subset.
        if (option.indexOf('=') <= 0 || option.indexOf('(') >= 0 || option.indexOf(')') >= 0
            || option.indexOf('\'') >= 0 || option.indexOf('"') >= 0) {
            throw unsupportedSyntax();
        }
        int equals = option.indexOf('=');
        if ("host".equalsIgnoreCase(decode(option.substring(0, equals).trim()))) {
            String host = decode(option.substring(equals + 1).trim());
            // Host fields within structured hosts are addresses, not another layer of property specifications.
            if (host.indexOf('=') >= 0 || host.indexOf('(') >= 0 || host.indexOf(')') >= 0) {
                throw unsupportedSyntax();
            }
        }
        validateAssignment(option);
    }

    private static void validateAssignment(@NotNull String option) throws DBCException {
        int equals = option.indexOf('=');
        String name = equals < 0 ? option : option.substring(0, equals);
        String value = equals < 0 ? "" : option.substring(equals + 1);
        validateOption(name, value);
    }

    private static void validateOption(@NotNull String name, @NotNull String value) throws DBCException {
        // Split first, then decode once. Encoded delimiters in values are data, not additional options.
        name = name.trim();
        String decodedName = decode(name);
        String decodedValue = decode(value.trim());
        for (String prohibited : BOOLEAN_PROPERTIES) {
            if ((prohibited.equalsIgnoreCase(name) || prohibited.equalsIgnoreCase(decodedName))
                && !"false".equalsIgnoreCase(decodedValue)) {
                throw prohibitedProperty(prohibited);
            }
        }
        if (INFILE_PATH.equalsIgnoreCase(name) || INFILE_PATH.equalsIgnoreCase(decodedName)) {
            throw prohibitedProperty(INFILE_PATH);
        }
        for (String indirect : INDIRECT_PROPERTIES) {
            if (indirect.equalsIgnoreCase(name) || indirect.equalsIgnoreCase(decodedName)) {
                throw prohibitedProperty(indirect);
            }
        }
        if ("HOST".equalsIgnoreCase(decodedName) || decodedName.matches("(?i)HOST\\.[0-9]+")) {
            // HOST/HOST.n can carry legacy address=(...) specifications in the Properties map too.
            validateHosts(decodedValue, 0);
        }
    }

    @NotNull
    private static String decode(@NotNull String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            // MariaDB allows literal '%' characters. MySQL rejects malformed encoding itself.
            return value;
        }
    }

    @NotNull
    private static DBCException prohibitedProperty(@NotNull String name) {
        // Never include the URL or option value: both may contain credentials or local paths.
        return new DBCException("JDBC driver parameter '" + name + "' is not allowed while driver parameter sanitization is enabled");
    }

    @NotNull
    private static DBCException unsupportedSyntax() {
        return new DBCException("Unsupported JDBC URL or host syntax while driver parameter sanitization is enabled");
    }
}
