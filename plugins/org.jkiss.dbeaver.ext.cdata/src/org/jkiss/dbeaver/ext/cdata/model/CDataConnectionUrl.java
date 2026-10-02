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
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.net.DBWUtils;

import java.util.Map;
import java.util.TreeMap;

public final class CDataConnectionUrl {
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
        Map<String, String> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        int position = 0;
        while (position < properties.length()) {
            char current = properties.charAt(position);
            if (current == ';' || Character.isWhitespace(current)) {
                position++;
                continue;
            }
            int equals = properties.indexOf('=', position);
            int separator = properties.indexOf(';', position);
            if (equals < 0 || separator >= 0 && separator < equals) {
                throw new DBException("Invalid CData connection property syntax");
            }
            String name = properties.substring(position, equals).trim();
            if (name.isEmpty()) {
                throw new DBException("Empty CData connection property name");
            }
            position = equals + 1;
            while (position < properties.length() && Character.isWhitespace(properties.charAt(position))) {
                position++;
            }
            StringBuilder value = new StringBuilder();
            char quote = position < properties.length() ? properties.charAt(position) : 0;
            if (quote == '\'' || quote == '"') {
                position++;
                boolean closed = false;
                while (position < properties.length()) {
                    current = properties.charAt(position++);
                    if (current == quote) {
                        if (position < properties.length() && properties.charAt(position) == quote) {
                            position++;
                        } else {
                            closed = true;
                            break;
                        }
                    }
                    value.append(current);
                }
                while (position < properties.length() && Character.isWhitespace(properties.charAt(position))) {
                    position++;
                }
                if (!closed || position < properties.length() && properties.charAt(position) != ';') {
                    throw new DBException("Invalid quoted CData connection property");
                }
            } else {
                int end = properties.indexOf(';', position);
                if (end < 0) {
                    end = properties.length();
                }
                value.append(properties.substring(position, end).trim());
                position = end;
            }
            result.put(name, value.toString());
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
