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
package org.jkiss.dbeaver.model.impl.jdbc;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.DBCException;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/**
 * Rejects restricted parameter names without invoking a driver or interpreting its URL grammar.
 */
public abstract class AbstractJDBCConnectionValidator implements JDBCConnectionValidator {
    /**
     * Returns names to reject in the URL and property names and values, ignoring case and checking
     * both literal text and text with one level of percent encoding decoded.
     */
    @NotNull
    protected abstract List<String> getRestrictedProperties();

    /**
     * Allows a restricted property name only when its supplied value is safe for the driver.
     */
    protected boolean isRestrictedPropertyAllowed(@NotNull Object name, @NotNull Object value) {
        return false;
    }

    @Override
    public void validateConnectionProperties(@Nullable String url, @NotNull Properties properties) throws DBCException {
        if (url == null) {
            throw new DBCException("Unable to validate JDBC driver parameters while driver parameter sanitization is enabled");
        }
        List<String> restrictedProperties = getRestrictedProperties();
        validateText(url, restrictedProperties);
        for (String name : properties.stringPropertyNames()) {
            validateProperty(name, properties.getProperty(name), restrictedProperties);
        }
        for (var entry : properties.entrySet()) {
            validateProperty(entry.getKey(), entry.getValue(), restrictedProperties);
        }
    }

    private void validateProperty(
        @NotNull Object name,
        @NotNull Object value,
        @NotNull List<String> restrictedProperties
    ) throws DBCException {
        if (!isRestrictedPropertyAllowed(name, value)) {
            validateText(String.valueOf(name), restrictedProperties);
        }
        validateText(String.valueOf(value), restrictedProperties);
    }

    private static void validateText(@NotNull String text, @NotNull List<String> restrictedProperties) throws DBCException {
        if (text.indexOf('%') >= 0) {
            try {
                text = URLDecoder.decode(text, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException ignored) {
                // Just a dangling percent sign, don't care
            }
        }
        checkRestrictedNames(text, restrictedProperties);
    }

    private static void checkRestrictedNames(@NotNull String text, @NotNull List<String> restrictedProperties) throws DBCException {
        String lowerText = text.toLowerCase(Locale.ROOT);
        for (String name : restrictedProperties) {
            if (lowerText.contains(name.toLowerCase(Locale.ROOT))) {
                throw new DBCException(
                    "JDBC driver parameter '" + name + "' is not allowed while driver parameter sanitization is enabled");
            }
        }
    }
}
