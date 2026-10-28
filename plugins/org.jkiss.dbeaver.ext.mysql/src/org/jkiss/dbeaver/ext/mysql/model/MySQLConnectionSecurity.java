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
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConnectionValidator;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Pattern;

/** Rejects prohibited parameter names without invoking a driver or interpreting its URL grammar. */
final class MySQLConnectionSecurity implements JDBCConnectionValidator {
    private static final List<String> MYSQL_URL_PREFIXES = List.of("jdbc:mysql:", "jdbc:mysql+srv:", "jdbc:mariadb:");
    private static final Pattern LITERAL_PERCENT = Pattern.compile("%(?![0-9a-fA-F]{2})");
    private static final List<String> BOOLEAN_PROPERTIES = List.of(
        "autoDeserialize", "allowLocalInfile", "allowLoadLocalInfile", "allowUrlInLocalInfile"
    );
    private static final List<String> PROHIBITED_PROPERTIES = List.of(
        "autoDeserialize", "allowLocalInfile", "allowLoadLocalInfileInPath", "allowLoadLocalInfile",
        "allowUrlInLocalInfile", "propertiesTransform", "useConfigs", "credentialType"
    );

    @Override
    public void validateConnectionProperties(@Nullable String url, @NotNull Properties properties) throws DBCException {
        if (url == null) {
            throw validationFailure();
        }
        validateText(url);
        // Include inherited defaults as well as non-string entries supplied by connection hooks.
        for (String name : properties.stringPropertyNames()) {
            validateText(name);
            validateText(properties.getProperty(name));
        }
        for (var entry : properties.entrySet()) {
            validateText(String.valueOf(entry.getKey()));
            validateText(String.valueOf(entry.getValue()));
        }
        // Reject user-supplied options first. MariaDB enables local infile by default when the option is absent.
        // Other drivers may reject unknown properties, so only default the MySQL/MariaDB URL families.
        String lowerUrl = decodeText(url).toLowerCase(Locale.ROOT);
        if (MYSQL_URL_PREFIXES.stream().anyMatch(lowerUrl::startsWith)) {
            BOOLEAN_PROPERTIES.forEach(name -> properties.setProperty(name, "false"));
        }
    }

    private static void validateText(@NotNull String text) throws DBCException {
        checkProhibitedNames(text);
        if (text.indexOf('%') >= 0) {
            checkProhibitedNames(decodeText(text));
        }
    }

    @NotNull
    private static String decodeText(@NotNull String text) {
        // Property values (especially passwords) can contain literal '%'. Preserve those while decoding
        // valid escapes once, so a literal percent cannot hide an encoded prohibited name later in the text.
        // This is not form data: '+' must remain literal, including in the mysql+srv URL scheme.
        String escapedText = LITERAL_PERCENT.matcher(text).replaceAll("%25").replace("+", "%2B");
        return URLDecoder.decode(escapedText, StandardCharsets.UTF_8);
    }

    private static void checkProhibitedNames(@NotNull String text) throws DBCException {
        String lowerText = text.toLowerCase(Locale.ROOT);
        for (String name : PROHIBITED_PROPERTIES) {
            if (lowerText.contains(name.toLowerCase(Locale.ROOT))) {
                throw new DBCException(
                    "JDBC driver parameter '" + name + "' is not allowed while driver parameter sanitization is enabled");
            }
        }
    }

    @NotNull
    private static DBCException validationFailure() {
        return new DBCException("Unable to validate JDBC driver parameters while driver parameter sanitization is enabled");
    }
}
