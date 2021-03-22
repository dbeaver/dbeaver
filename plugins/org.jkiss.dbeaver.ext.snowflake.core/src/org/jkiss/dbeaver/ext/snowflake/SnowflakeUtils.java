/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.snowflake;

import org.jkiss.code.NotNull;

/**
 * Snowflake utils.
 */
public class SnowflakeUtils {
    private SnowflakeUtils() {

    }

    /**
     * Determines if identifier is case sensitive / must be enclosed in double quotes (in Swnoflake world
     * that's the same thing).
     *
     * See https://docs.snowflake.com/en/sql-reference/identifiers-syntax.html
     *
     * @param identifier snowflake identifier
     * @return {@code true} if identifier is case sensitive and should be used enclosed in double quotes
     * @throws IllegalArgumentException if identifier is an empty string
     */
    public static boolean isCaseSensitiveIdentifier(@NotNull String identifier) {
        if (identifier.isEmpty() || identifier.length() > 255) {
            throw new IllegalArgumentException("Empty string is an illegal Snowflake identifier");
        }
        char firstChar = identifier.charAt(0);
        if (!Character.isLetter(firstChar) && firstChar != '_') {
            return true;
        }
        for (int i = 1; i < identifier.length(); i++) {
            char c = identifier.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '_' && c != '$') {
                return true;
            }
        }
        return false;
    }

    /**
     * Escapes snowflake identifier (if needed), making it ready for use in SQL queries.
     *
     * @param identifier snowflake identifier
     * @return identifier, ready for use in SQL queries
     * @throws IllegalArgumentException if identifier is an empty string
     */
    @NotNull
    public static String escapeIdentifier(@NotNull String identifier) {
        if (isCaseSensitiveIdentifier(identifier)) {
            identifier = identifier.replace("\"", "\"\"");
            return "\"" + identifier + "\"";
        }
        return identifier;
    }
}
