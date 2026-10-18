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
package org.jkiss.dbeaver.model.sql.completion.usage;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Objects and tokens of one query, as learned by {@link SQLCompletionUsageModel}.
 *
 * <p>Tokens are the "semantic stream" of the query: keywords (upper case) and object keys, without literals,
 * aliases and punctuation. E.g. {@code SELECT a.id FROM orders a JOIN items i ON ...} becomes
 * {@code SELECT c:s.orders.id FROM t:s.orders JOIN t:s.items ON ...}.</p>
 */
public final class SQLUsageQuery {
    static final String TABLE_PREFIX = "t:";
    static final String COLUMN_PREFIX = "c:";
    static final String SCHEMA_PREFIX = "s:";

    private final Set<String> tables = new LinkedHashSet<>();
    private final Set<String> objects = new LinkedHashSet<>();
    private final List<String> tokens = new ArrayList<>();
    // Names of the tables (container path and table name, in the original case), to find them again
    private final Map<String, List<String>> tablePaths = new LinkedHashMap<>();

    @NotNull
    public static String tableKey(@Nullable String schema, @NotNull String table) {
        return TABLE_PREFIX + normalize(schema) + "." + normalize(table);
    }

    @NotNull
    public static String schemaKey(@NotNull String schema) {
        return SCHEMA_PREFIX + normalize(schema);
    }

    @NotNull
    public static String columnKey(@Nullable String schema, @NotNull String table, @NotNull String column) {
        return COLUMN_PREFIX + normalize(schema) + "." + normalize(table) + "." + normalize(column);
    }

    /**
     * Table key of a table or column key, or null for other tokens.
     */
    @Nullable
    public static String tableOf(@NotNull String key) {
        if (key.startsWith(TABLE_PREFIX)) {
            return key;
        }
        if (key.startsWith(COLUMN_PREFIX)) {
            int pos = key.lastIndexOf('.');
            return pos < 0 ? null : TABLE_PREFIX + key.substring(COLUMN_PREFIX.length(), pos);
        }
        return null;
    }

    @NotNull
    private static String normalize(@Nullable String name) {
        return name == null ? "" : name.toLowerCase(Locale.ENGLISH);
    }

    /**
     * Adds a keyword to the token stream.
     */
    public void addKeyword(@NotNull String keyword) {
        tokens.add(keyword.toUpperCase(Locale.ENGLISH));
    }

    /**
     * Adds a table reference (a token and a used object).
     */
    public void addTable(@NotNull String tableKey) {
        tables.add(tableKey);
        objects.add(tableKey);
        tokens.add(tableKey);
        // The schema is used with the table
        int pos = tableKey.indexOf('.');
        if (pos > TABLE_PREFIX.length()) {
            objects.add(SCHEMA_PREFIX + tableKey.substring(TABLE_PREFIX.length(), pos));
        }
    }

    /**
     * Adds a table reference with the names of its containers and its own name (e.g. schema and table),
     * so the table can be proposed again in other queries.
     */
    public void addTable(@NotNull String tableKey, @NotNull List<String> path) {
        addTable(tableKey);
        tablePaths.put(tableKey, List.copyOf(path));
    }

    @NotNull
    public Map<String, List<String>> getTablePaths() {
        return Collections.unmodifiableMap(tablePaths);
    }

    /**
     * Adds a column reference (a token and a used object).
     */
    public void addColumn(@NotNull String columnKey) {
        objects.add(columnKey);
        tokens.add(columnKey);
    }

    @NotNull
    public List<String> getTables() {
        return new ArrayList<>(tables);
    }

    @NotNull
    public Set<String> getObjects() {
        return Collections.unmodifiableSet(objects);
    }

    @NotNull
    public List<String> getTokens() {
        return Collections.unmodifiableList(tokens);
    }
}
