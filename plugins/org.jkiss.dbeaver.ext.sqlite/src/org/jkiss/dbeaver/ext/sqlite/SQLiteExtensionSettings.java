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
package org.jkiss.dbeaver.ext.sqlite;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.StringTemplate;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DataSourceVariableResolver;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Properties;

public record SQLiteExtensionSettings(
    @NotNull String connectionKey,
    @NotNull String fingerprint,
    @NotNull String connectionName,
    @NotNull String url,
    @NotNull List<String> libraries,
    @NotNull List<String> initQueries,
    boolean enabled,
    boolean temporary,
    @NotNull String temporaryKey
) {
    @NotNull
    public static SQLiteExtensionSettings capture(@NotNull DBPDataSourceContainer container) throws DBException {
        DBPConnectionConfiguration configuration = resolvedConfiguration(container);
        String url = configuration.getUrl();
        if (url == null || url.isEmpty()) {
            try {
                url = container.getDriver().getConnectionURL(configuration);
            } catch (DBException exception) {
                if (!(CommonUtils.getRootCause(exception) instanceof StringTemplate.StringTemplateMissingParametersException)) {
                    throw exception;
                }
                url = "";
            }
        }
        Properties properties = new Properties();
        String enabled = configuration.getProviderProperty(SQLiteExtensionLoading.PROP_ENABLED);
        if (enabled != null) {
            properties.setProperty(SQLiteExtensionLoading.ENABLE_LOAD_EXTENSION, enabled);
        } else if (CommonUtils.isNotEmpty(configuration.getProviderProperty(SQLiteExtensionLoading.PROP_EXTENSIONS))) {
            properties.setProperty(SQLiteExtensionLoading.ENABLE_LOAD_EXTENSION, Boolean.TRUE.toString());
        }
        container.getDriver().getConnectionProperties().forEach((key, value) -> {
            if (value != null) {
                properties.setProperty(key, value.toString());
            }
        });
        configuration.getProperties().forEach((key, value) -> {
            if (value != null) {
                properties.setProperty(key, value);
            }
        });
        return capture(container, configuration, url, properties);
    }

    @NotNull
    public static SQLiteExtensionSettings capture(
        @NotNull DBPDataSourceContainer container,
        @Nullable String url,
        @NotNull Properties properties
    ) {
        return capture(container, new DBPConnectionConfiguration(container.getActualConnectionConfiguration()), url, properties);
    }

    @NotNull
    private static SQLiteExtensionSettings capture(
        @NotNull DBPDataSourceContainer container,
        @NotNull DBPConnectionConfiguration configuration,
        @Nullable String url,
        @NotNull Properties properties
    ) {
        String requested = configuration.getProviderProperty(SQLiteExtensionLoading.PROP_ENABLED);
        boolean enabled = requested == null
            ? SQLiteExtensionLoading.isEnabled(url, properties) : Boolean.parseBoolean(requested);
        String extensions = configuration.getProviderProperty(SQLiteExtensionLoading.PROP_EXTENSIONS);
        List<String> libraries = extensions == null || extensions.isEmpty()
            ? List.of() : List.of(extensions.split(java.io.File.pathSeparator));
        DataSourceVariableResolver resolver = new DataSourceVariableResolver(container, configuration);
        List<String> queries = configuration.getBootstrap().getInitQueries().stream()
            .map(query -> GeneralUtils.replaceVariables(query, resolver)).toList();
        String connectionUrl = url == null ? "" : url;
        String temporaryKey = digest(List.of(
            container.getProject().getAbsolutePath().toAbsolutePath().normalize().toString(), container.getId()));
        DBPDataSourceContainer identity = container;
        Object source = configuration.getRuntimeAttribute(DBPConnectionConfiguration.ATTRIBUTE_TEST_CONNECTION_SOURCE);
        if (container.isTemporary() && source instanceof DBPDataSourceContainer original) {
            identity = original;
        }
        String key = digest(List.of(
            identity.getProject().getAbsolutePath().toAbsolutePath().normalize().toString(),
            identity.getId()
        ));
        String fingerprint = digest(List.of(
            container.getDriver().getFullId(),
            container.getDriver().getDriverClassName(),
            connectionUrl,
            Boolean.toString(enabled),
            requested == null ? "" : requested,
            properties.getProperty(SQLiteExtensionLoading.ENABLE_LOAD_EXTENSION, ""),
            digest(libraries),
            digest(queries)
        ));
        return new SQLiteExtensionSettings(key, fingerprint, container.getName(), connectionUrl, libraries, queries,
            enabled, container.isTemporary(), temporaryKey);
    }

    @NotNull
    private static DBPConnectionConfiguration resolvedConfiguration(@NotNull DBPDataSourceContainer container) {
        DBPConnectionConfiguration configuration = new DBPConnectionConfiguration(container.getConnectionConfiguration());
        if (container.getPreferenceStore().getBoolean(ModelPreferences.CONNECT_USE_ENV_VARS)) {
            configuration.resolveDynamicVariables(new DataSourceVariableResolver(container, configuration));
        }
        return configuration;
    }

    @NotNull
    private static String digest(@NotNull List<String> values) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String value : values) {
                byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
                digest.update(Integer.toString(bytes.length).getBytes(StandardCharsets.US_ASCII));
                digest.update((byte) ':');
                digest.update(bytes);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
