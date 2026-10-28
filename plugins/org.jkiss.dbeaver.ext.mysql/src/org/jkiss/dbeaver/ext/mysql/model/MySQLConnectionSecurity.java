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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.exec.DBCException;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/** Applies connection policy to driver-parsed options, without duplicating JDBC URL grammars. */
final class MySQLConnectionSecurity {
    private static final Log log = Log.getLog(MySQLConnectionSecurity.class);
    private static final List<String> BOOLEAN_PROPERTIES = List.of(
        "autoDeserialize", "allowLocalInfile", "allowLoadLocalInfile", "allowUrlInLocalInfile"
    );
    private static final List<String> INDIRECT_PROPERTIES = List.of("propertiesTransform", "useConfigs", "credentialType");
    private static final String INFILE_PATH = "allowLoadLocalInfileInPath";

    private MySQLConnectionSecurity() {
    }

    static void sanitizeProperties(@NotNull Properties properties) {
        // MariaDB resolves option names case-insensitively, including differently cased duplicates.
        properties.keySet().removeIf(key -> key instanceof String name
            && (INFILE_PATH.equalsIgnoreCase(name) || BOOLEAN_PROPERTIES.stream().anyMatch(p -> p.equalsIgnoreCase(name))));
        BOOLEAN_PROPERTIES.forEach(name -> properties.setProperty(name, "false"));
    }

    static void validate(@Nullable Driver driver, @Nullable String url, @NotNull Properties properties) throws DBCException {
        for (String name : properties.stringPropertyNames()) {
            validateOption(name, properties.getProperty(name));
        }
        if (url == null) {
            throw validationFailure();
        }
        try {
            if (driver == null) {
                // DriverManager.getDriver invokes acceptsURL, which may itself parse configuration.
                checkBeforeConfigurationParsing(URLDecoder.decode(url, StandardCharsets.UTF_8));
                driver = DriverManager.getDriver(url);
            }
            Class<?> driverClass = driver.getClass();
            try {
                if (isDriver(driverClass, "com.mysql.cj.jdbc.NonRegisteringDriver")) {
                    validateMySQLUrl(url, driverClass.getClassLoader().loadClass("com.mysql.cj.conf.ConnectionUrlParser"));
                } else if (isDriver(driverClass, "com.mysql.jdbc.NonRegisteringDriver")) {
                    validateLegacyUrl(driver, url, properties);
                } else if (isDriver(driverClass, "org.mariadb.jdbc.Driver")) {
                    validateMariaDBUrl(url, properties, driverClass.getClassLoader().loadClass("org.mariadb.jdbc.Configuration"));
                } else {
                    // Driver substitutions and wrappers need their own URL interpretation, not MySQL's grammar.
                    validateDriverProperties(driver, url, properties);
                }
            } catch (ClassNotFoundException | NoSuchMethodException e) {
                // Driver-specific APIs are not JDBC contracts. Keep versions without them usable.
                validateDriverProperties(driver, url, properties);
            }
        } catch (ReflectiveOperationException | SQLException | RuntimeException | LinkageError e) {
            // Driver/parser exceptions can contain the complete URL, credentials, or local paths.
            throw validationFailure();
        }
    }

    static void validateMySQLUrl(@NotNull String url, @NotNull Class<?> parserClass)
        throws ReflectiveOperationException, DBCException {
        // ConnectionUrlParser only parses syntax. ConnectionUrl would execute property transformers.
        Object parser = parserClass.getMethod("parseConnectionString", String.class).invoke(null, url);
        validateOptions((Map<?, ?>) parserClass.getMethod("getProperties").invoke(parser));
        // Both getters are required: authority and query parsing are lazy in Connector/J.
        for (Object host : (List<?>) parserClass.getMethod("getHosts").invoke(parser)) {
            validateOptions((Map<?, ?>) host.getClass().getMethod("getHostProperties").invoke(host));
        }
    }

    static void validateLegacyUrl(@NotNull Driver driver, @NotNull String url, @NotNull Properties properties)
        throws ReflectiveOperationException, DBCException {
        checkBeforeConfigurationParsing(url);
        Class<?> driverClass = driver.getClass();
        Properties parsed = (Properties) driverClass.getMethod("parseURL", String.class, Properties.class)
            .invoke(driver, url, properties.clone());
        if (parsed == null) {
            throw validationFailure();
        }
        validateOptions(parsed);
        // Legacy Connector/J expands host specifications after merging the supplied Properties.
        for (String name : parsed.stringPropertyNames()) {
            if (name.equals("HOST") || name.startsWith("HOST.")) {
                String host = parsed.getProperty(name);
                if (Boolean.TRUE.equals(driverClass.getMethod("isHostPropertiesList", String.class).invoke(null, host))) {
                    validateOptions((Properties) driverClass.getMethod("expandHostKeyValues", String.class).invoke(null, host));
                }
            }
        }
    }

    static void validateMariaDBUrl(@NotNull String url, @NotNull Properties properties, @NotNull Class<?> configurationClass)
        throws ReflectiveOperationException, DBCException {
        checkBeforeConfigurationParsing(url);
        // Configuration construction may initialize trusted, locally installed codec providers.
        // Prevent URL-selected credential plugins first, and isolate the driver's mutation of Properties.
        Object configuration = configurationClass.getMethod("parse", String.class, Properties.class)
            .invoke(null, url, properties.clone());
        if (Boolean.TRUE.equals(configurationClass.getMethod("allowLocalInfile").invoke(configuration))) {
            throw prohibitedProperty("allowLocalInfile");
        }
    }

    private static void validateDriverProperties(@NotNull Driver driver, @NotNull String url, @NotNull Properties properties)
        throws SQLException, DBCException {
        checkBeforeConfigurationParsing(URLDecoder.decode(url, StandardCharsets.UTF_8));
        log.warn("No dedicated JDBC parameter parser for " + driver.getClass().getName()
            + "; validation is limited to the properties exposed by the driver");
        DriverPropertyInfo[] options = driver.getPropertyInfo(url, (Properties) properties.clone());
        if (options != null) {
            for (DriverPropertyInfo option : options) {
                if (option.value != null) {
                    validateOption(option.name, option.value);
                }
            }
        }
    }

    private static void checkBeforeConfigurationParsing(@NotNull String url) throws DBCException {
        // These APIs mix parsing with execution. A conservative token guard is intentional: do not
        // invoke them to discover the very option that could cause extension code to execute.
        String lowerUrl = url.toLowerCase(Locale.ROOT);
        for (String name : INDIRECT_PROPERTIES) {
            if (lowerUrl.contains(name.toLowerCase(Locale.ROOT))) {
                throw prohibitedProperty(name);
            }
        }
    }

    private static boolean isDriver(@NotNull Class<?> driverClass, @NotNull String baseClassName) {
        for (Class<?> type = driverClass; type != null; type = type.getSuperclass()) {
            if (type.getName().equals(baseClassName)) {
                return true;
            }
        }
        return false;
    }

    private static void validateOptions(@NotNull Map<?, ?> options) throws DBCException {
        for (Map.Entry<?, ?> option : options.entrySet()) {
            validateOption((String) option.getKey(), (String) option.getValue());
        }
    }

    private static void validateOption(@NotNull String name, @Nullable String value) throws DBCException {
        // Driver-produced names and values are already decoded. Never decode or tokenize them again.
        for (String prohibited : BOOLEAN_PROPERTIES) {
            if (prohibited.equalsIgnoreCase(name) && !"false".equalsIgnoreCase(value)) {
                throw prohibitedProperty(prohibited);
            }
        }
        if (INFILE_PATH.equalsIgnoreCase(name)) {
            throw prohibitedProperty(INFILE_PATH);
        }
        for (String indirect : INDIRECT_PROPERTIES) {
            if (indirect.equalsIgnoreCase(name)) {
                throw prohibitedProperty(indirect);
            }
        }
    }

    @NotNull
    private static DBCException prohibitedProperty(@NotNull String name) {
        return new DBCException("JDBC driver parameter '" + name + "' is not allowed while driver parameter sanitization is enabled");
    }

    @NotNull
    private static DBCException validationFailure() {
        return new DBCException("Unable to validate JDBC driver parameters while driver parameter sanitization is enabled");
    }
}
