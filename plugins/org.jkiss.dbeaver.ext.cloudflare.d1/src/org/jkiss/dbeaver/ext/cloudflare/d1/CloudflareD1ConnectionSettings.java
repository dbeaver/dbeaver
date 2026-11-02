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
package org.jkiss.dbeaver.ext.cloudflare.d1;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriverConfigurationType;

import java.net.URI;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class CloudflareD1ConnectionSettings {
    public static final String DATABASE_IDS = "databaseIds";
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z0-9_-]+");

    private CloudflareD1ConnectionSettings() {
    }

    @NotNull
    public static Validation validate(@NotNull String initialId, boolean filtered, @NotNull String additionalIds) {
        if (initialId.isBlank()) {
            return Validation.INITIAL_ID_REQUIRED;
        }
        if (!IDENTIFIER.matcher(initialId.trim()).matches()) {
            return Validation.INVALID_INITIAL_ID;
        }
        if (filtered && !additionalIds.isBlank()) {
            for (String id : additionalIds.split(",", -1)) {
                if (id.isBlank()) {
                    return Validation.EMPTY_SCOPE_ENTRY;
                }
                if (!IDENTIFIER.matcher(id.trim()).matches()) {
                    return Validation.INVALID_SCOPE_ID;
                }
            }
        }
        return Validation.VALID;
    }

    @NotNull
    public static String serializeScope(@NotNull String initialId, @NotNull String additionalIds) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        ids.add(initialId.trim());
        if (!additionalIds.isBlank()) {
            for (String id : additionalIds.split(",", -1)) {
                ids.add(id.trim());
            }
        }
        return String.join(",", ids);
    }

    @NotNull
    public static String additionalIds(@NotNull String initialId, @NotNull String scope) {
        return Arrays.stream(scope.split(",", -1))
            .map(String::trim)
            .filter(id -> !id.equals(initialId.trim()))
            .collect(Collectors.joining(","));
    }

    public static boolean removeDatabaseFromScope(@NotNull DBPConnectionConfiguration configuration, @NotNull String databaseId) {
        String scope = configuration.getProperty(DATABASE_IDS);
        if (scope == null || scope.isBlank()) {
            return false;
        }
        String updated = Arrays.stream(serializeScope(configuration.getDatabaseName(), scope).split(","))
            .filter(id -> !id.equals(databaseId))
            .collect(Collectors.joining(","));
        if (scope.equals(updated)) {
            return false;
        }
        // Keep a scope containing only the initial database: removing the property would enumerate the whole account.
        configuration.setProperty(DATABASE_IDS, updated);
        return true;
    }

    public static void save(
        @NotNull DBPConnectionConfiguration configuration, @NotNull String initialId, boolean filtered, @NotNull String additionalIds
    ) {
        configuration.setConfigurationType(DBPDriverConfigurationType.MANUAL);
        configuration.setDatabaseName(initialId.trim());
        configuration.setUrl("");
        if (filtered) {
            configuration.setProperty(DATABASE_IDS, serializeScope(initialId, additionalIds));
        } else {
            configuration.getProperties().remove(DATABASE_IDS);
        }
        removeLegacyCredentials(configuration);
    }

    public static void removeLegacyCredentials(@NotNull DBPConnectionConfiguration configuration) {
        configuration.getProperties().remove("accountId");
        configuration.getProperties().remove("apiToken");
    }

    public static boolean isInsecureEndpoint(@NotNull String endpoint) {
        try {
            URI uri = URI.create(endpoint.trim());
            String host = uri.getHost();
            return "http".equalsIgnoreCase(uri.getScheme())
                && host != null
                && !"localhost".equalsIgnoreCase(host)
                && !host.startsWith("127.")
                && !"[::1]".equals(host)
                && !"::1".equals(host);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public enum Validation {
        VALID,
        INITIAL_ID_REQUIRED,
        INVALID_INITIAL_ID,
        EMPTY_SCOPE_ENTRY,
        INVALID_SCOPE_ID
    }
}
