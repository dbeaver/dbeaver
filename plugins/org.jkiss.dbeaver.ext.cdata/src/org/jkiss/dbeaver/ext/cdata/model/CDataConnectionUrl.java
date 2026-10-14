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
package org.jkiss.dbeaver.ext.cdata.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DatabaseURL;
import org.jkiss.dbeaver.model.StringTemplate;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.net.DBWUtils;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class CDataConnectionUrl {
    private static final String PROPERTIES_TEMPLATE = "{separator}[{param:{prop}={value}{separator}}...]";
    private static final DatabaseURL.Pattern PROPERTIES_PATTERN;

    static {
        try {
            PROPERTIES_PATTERN = DatabaseURL.getUrlPattern(PROPERTIES_TEMPLATE, param -> switch (param.name()) {
                case "prop" -> "[^;=]*[^;=\\p{javaWhitespace}]\\p{javaWhitespace}*";
                case "value" -> "\\p{javaWhitespace}*+(?:\"(?:[^\"]++|\"\")*+\"|'(?:[^']++|'')*+'|(?!['\"])[^;]*)"
                    + "(?=\\p{javaWhitespace}*(?:;|\\z))";
                case "separator" -> "[;\\p{javaWhitespace}]*";
                default -> throw new IllegalArgumentException("Unknown CData URL template parameter");
            });
        } catch (StringTemplate.StringTemplateFormatException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private CDataConnectionUrl() {
    }

    @NotNull
    public static Map<String, String> parse(@NotNull String url, @NotNull String source) throws DBException {
        String prefix = "jdbc:" + source + ":";
        String fullPrefix = "jdbc:cdata:" + source + ":";
        String properties;
        if (url.isBlank()) {
            properties = "";
        } else if (url.regionMatches(true, 0, fullPrefix, 0, fullPrefix.length())) {
            properties = url.substring(fullPrefix.length());
        } else if (url.regionMatches(true, 0, prefix, 0, prefix.length())) {
            properties = url.substring(prefix.length());
        } else {
            throw new DBException("The JDBC URL does not match this CData driver");
        }
        var entries = PROPERTIES_PATTERN.tryRecognizeHierarchical(properties, true);
        if (entries == null) {
            throw new DBException("Invalid CData connection property syntax");
        }
        Map<String, String> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        // template captures are enumerated backwards; preserve the last value for duplicate properties
        var propertyEntries = entries.getGroups().getOrDefault("param", List.of()).reversed();
        for (var entry : propertyEntries) {
            String name = entry.getFirstParamValue("prop").trim();
            String value = entry.getFirstParamValue("value").stripLeading();
            if (!value.isEmpty() && (value.charAt(0) == '\'' || value.charAt(0) == '"')) {
                String quote = value.substring(0, 1);
                value = value.substring(1, value.length() - 1).replace(quote + quote, quote);
            } else {
                value = value.trim();
            }
            result.put(name, value);
        }
        return result;
    }

    public static void updateEndpoint(
        @NotNull String source,
        @NotNull DBPConnectionConfiguration configuration,
        @NotNull Map<String, String> defaults
    ) throws DBException {
        Map<String, String> properties = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        properties.putAll(defaults);
        properties.putAll(configuration.getProperties());
        properties.putAll(parse(configuration.getUrl() == null ? "" : configuration.getUrl(), source));
        configuration.setHostName(properties.getOrDefault("Server", properties.get("Host")));
        configuration.setHostPort(properties.get("Port"));
        configuration.setDatabaseName(properties.get("Database"));
    }

    @NotNull
    public static String getConnectionUrl(@NotNull String source, @NotNull DBPConnectionConfiguration configuration) throws DBException {
        String url = configuration.getUrl() == null ? "jdbc:" + source + ":" : configuration.getUrl();
        if (DBWUtils.getTunnelConfiguration(configuration) == null) {
            return url;
        }
        var properties = parse(url, source);
        Map<String, String> effective = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        effective.putAll(configuration.getProperties());
        effective.putAll(properties);
        String hostProperty = effective.containsKey("Server") ? "Server" : effective.containsKey("Host") ? "Host" : null;
        if (hostProperty == null || configuration.getHostName() == null || configuration.getHostName().isBlank()
            || configuration.getHostPort() == null || configuration.getHostPort().isBlank()) {
            throw new DBException("CData tunneling requires a Server or Host property and a database port");
        }
        properties.put(hostProperty, configuration.getHostName());
        properties.put("Port", configuration.getHostPort());
        // tunnel configuration is a copy; driver properties must not override its local endpoint
        configuration.getProperties().replaceAll((name, value) -> {
            if (name.equalsIgnoreCase(hostProperty)) {
                return configuration.getHostName();
            }
            return name.equalsIgnoreCase("Port") ? configuration.getHostPort() : value;
        });
        return build(source, properties);
    }

    @NotNull
    public static String build(@NotNull String source, @NotNull Map<String, String> properties) {
        StringBuilder url = new StringBuilder("jdbc:" + source + ":");
        properties.forEach((name, value) -> {
            url.append(name).append('=');
            if (value.indexOf(';') >= 0 || value.indexOf('"') >= 0 || value.indexOf('\'') >= 0 || !value.equals(value.strip())) {
                url.append('"').append(value.replace("\"", "\"\"")).append('"');
            } else {
                url.append(value);
            }
            url.append(';');
        });
        return url.toString();
    }
}
