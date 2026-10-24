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
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolByDbObjectDefinition;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolEntry;
import org.jkiss.dbeaver.model.sql.semantics.completion.SQLQueryCompletionContext;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryModel;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsTableDataModel;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Builds the usage of a recognized query: tables and columns resolved by the semantic analysis, in text order,
 * with the keywords which give their role (SELECT, FROM, JOIN, ON, WHERE...).
 */
public final class SQLUsageExtractor {
    // Keywords kept in the token stream. Others (and literals, aliases, operators) are left out.
    private static final Set<String> KEYWORDS = Set.of(
        "SELECT", "FROM", "JOIN", "ON", "WHERE", "GROUP", "ORDER", "BY", "HAVING", "AND", "OR",
        "INSERT", "INTO", "UPDATE", "SET", "DELETE", "VALUES", "UNION", "USING");

    private SQLUsageExtractor() {
    }

    private record Token(int position, @NotNull String keyword, @Nullable DBSObject object) {
    }

    /**
     * Usage of the query text before {@code end} (the whole query for an executed query, the text before the cursor
     * for a completion request).
     */
    @NotNull
    public static SQLUsageQuery extract(@NotNull SQLQueryModel model, @NotNull String text, int end) {
        List<Token> tokens = new ArrayList<>();
        List<int[]> symbolRanges = new ArrayList<>();
        for (SQLQuerySymbolEntry entry : model.getAllSymbols()) {
            int start = entry.getInterval().a;
            if (start < 0 || start >= end || (end < text.length() && entry.getInterval().b + 1 >= end)) {
                // After the end, or the name being typed at the cursor
                continue;
            }
            symbolRanges.add(new int[]{start, entry.getInterval().b});
            DBSObject object = resolve(entry);
            if (object instanceof DBSEntity || object instanceof DBSEntityAttribute) {
                tokens.add(new Token(start, "", object));
            }
        }
        scanKeywords(text, Math.min(end, text.length()), symbolRanges, tokens);
        tokens.sort(Comparator.comparingInt(Token::position));

        SQLUsageQuery query = new SQLUsageQuery();
        for (Token token : tokens) {
            if (token.object instanceof DBSEntity entity) {
                query.addTable(tableKey(entity), tablePath(entity));
            } else if (token.object instanceof DBSEntityAttribute attribute) {
                String key = columnKey(attribute);
                if (key != null) {
                    query.addColumn(key);
                }
            } else {
                query.addKeyword(token.keyword);
            }
        }
        return query;
    }

    /**
     * Usage context of a completion request: tables of the query and the tokens before the cursor.
     */
    @NotNull
    public static SQLUsageContext prepareContext(@NotNull SQLQueryCompletionContext context) {
        List<String> tables = new ArrayList<>();
        for (DBSEntity table : context.getDataContext().getKnownSources().getReferencedTables()) {
            tables.add(tableKey(table));
        }
        List<String> previous = List.of();
        SQLQueryModel model = context.getQueryModel();
        String text = context.getQueryText();
        if (model != null && text != null) {
            int end = Math.max(0, Math.min(context.getRequestOffset() - context.getOffset(), text.length()));
            previous = extract(model, text, end).getTokens();
        }
        return new SQLUsageContext(tables, previous);
    }

    /**
     * Key of a table proposal or a column proposal, or null for other objects.
     */
    @Nullable
    public static String objectKey(@Nullable DBSObject object) {
        if (object instanceof DBSEntity entity) {
            return tableKey(entity);
        }
        if (object instanceof DBSSchema || object instanceof DBSCatalog) {
            return SQLUsageQuery.schemaKey(object.getName());
        }
        if (object instanceof DBSEntityAttribute attribute) {
            return columnKey(attribute);
        }
        return null;
    }

    @NotNull
    static String tableKey(@NotNull DBSEntity entity) {
        DBSObject parent = entity.getParentObject();
        return SQLUsageQuery.tableKey(parent == null ? null : parent.getName(), entity.getName());
    }

    /**
     * Names of the containers of the table (from the data source, e.g. catalog and schema) and of the table itself.
     */
    @NotNull
    static List<String> tablePath(@NotNull DBSEntity entity) {
        LinkedList<String> path = new LinkedList<>();
        for (DBSObject object = entity;
             object != null && !(object instanceof DBPDataSource) && !(object instanceof DBPDataSourceContainer);
             object = object.getParentObject()) {
            path.addFirst(object.getName());
        }
        return path;
    }

    @Nullable
    static String columnKey(@NotNull DBSEntityAttribute attribute) {
        DBSEntity table = attribute.getParentObject();
        if (table == null) {
            return null;
        }
        DBSObject parent = table.getParentObject();
        return SQLUsageQuery.columnKey(parent == null ? null : parent.getName(), table.getName(), attribute.getName());
    }

    @Nullable
    private static DBSObject resolve(@NotNull SQLQuerySymbolEntry entry) {
        // Aliases and derived columns point to the entry they come from
        while (entry.getDefinition() instanceof SQLQuerySymbolEntry definition && definition != entry) {
            entry = definition;
        }
        return switch (entry.getDefinition()) {
            case SQLQuerySymbolByDbObjectDefinition definition -> definition.getDbObject();
            case SQLQueryRowsTableDataModel table -> table.getImmediateTargetObject();
            case null, default -> null;
        };
    }

    /**
     * Finds keywords outside of string literals, quoted identifiers, comments and symbols.
     */
    private static void scanKeywords(@NotNull String text, int end, @NotNull List<int[]> symbolRanges, @NotNull List<Token> tokens) {
        int i = 0;
        while (i < end) {
            char c = text.charAt(i);
            if (c == '\'' || c == '"' || c == '`' || c == '[') {
                char close = c == '[' ? ']' : c;
                int j = text.indexOf(close, i + 1);
                i = j < 0 ? end : j + 1;
            } else if (c == '-' && i + 1 < end && text.charAt(i + 1) == '-') {
                int j = text.indexOf('\n', i);
                i = j < 0 ? end : j + 1;
            } else if (c == '/' && i + 1 < end && text.charAt(i + 1) == '*') {
                int j = text.indexOf("*/", i + 2);
                i = j < 0 ? end : j + 2;
            } else if (Character.isLetter(c) || c == '_') {
                int j = i;
                while (j < end && (Character.isLetterOrDigit(text.charAt(j)) || text.charAt(j) == '_')) {
                    j++;
                }
                // A word ending at the cursor may be incomplete
                boolean complete = j < end || j == text.length();
                String word = text.substring(i, j).toUpperCase(Locale.ENGLISH);
                if (complete && KEYWORDS.contains(word) && !insideSymbol(i, symbolRanges)) {
                    tokens.add(new Token(i, word, null));
                }
                i = j;
            } else {
                i++;
            }
        }
    }

    private static boolean insideSymbol(int position, @NotNull List<int[]> symbolRanges) {
        for (int[] range : symbolRanges) {
            if (position >= range[0] && position <= range[1]) {
                return true;
            }
        }
        return false;
    }
}
