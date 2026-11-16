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
package org.jkiss.dbeaver.model.ai.functions;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Best-effort reference extraction, not SQL validation. Unknown syntax is tolerated so that
 * recognizable references in vendor extensions and incomplete statements can still be checked.
 * An empty result does not establish that a statement accesses no database objects.
 */
public final class SQLScopeHeuristics {
    private static final Pattern WORD = Pattern.compile("[\\p{L}_$#@][\\p{L}\\p{N}_$#@]*");
    private static final Pattern DOLLAR_QUOTE = Pattern.compile("\\$(?:[A-Za-z_][A-Za-z_0-9]*)?\\$");
    private static final Set<String> TABLE_PREFIXES = Set.of(
        "FROM", "JOIN", "UPDATE", "INTO", "USING", "TABLE", "VIEW", "REFERENCES", "TRUNCATE",
        "DESCRIBE", "DESC", "COPY", "ANALYZE", "OPTIMIZE", "REPAIR", "CALL", "EXEC", "EXECUTE"
    );
    private static final Set<String> MODIFIERS = Set.of(
        "IF", "NOT", "EXISTS", "ONLY", "LATERAL", "LOW_PRIORITY", "IGNORE", "OVERWRITE", "TABLE"
    );
    private static final Set<String> END_FROM = Set.of("WHERE", "GROUP", "ORDER", "HAVING", "LIMIT", "OFFSET",
        "FETCH", "SET", "VALUES", "RETURNING", "UNION", "EXCEPT", "INTERSECT", "QUALIFY", "WINDOW");
    private static final Set<String> NOT_NAMES = Set.of(
        "SELECT", "WITH", "AS", "ON", "TO", "FOR", "BY", "INNER", "OUTER", "LEFT", "RIGHT", "FULL",
        "CROSS", "NATURAL", "PARTITION", "DEFAULT", "FUNCTION", "PROCEDURE", "IMMEDIATE",
        "STDIN", "STDOUT", "OUTFILE", "DUMPFILE", "CASCADE", "RESTRICT", "NO", "ACTION"
    );
    private static final Set<String> INLINE_TABLE_FUNCTIONS = Set.of(
        "UNNEST", "JSON_TABLE", "OPENJSON", "GENERATE_SERIES", "GENERATOR", "FLATTEN", "JSONB_TO_RECORDSET"
    );

    private SQLScopeHeuristics() {
    }

    @NotNull
    public static Set<String> extractTableNames(@NotNull String sql) {
        return extractTableNames(sql, 0);
    }

    @NotNull
    private static Set<String> extractTableNames(@NotNull String sql, int recursion) {
        List<String> tokens = tokenize(sql);
        Set<String> names = new LinkedHashSet<>();
        if (tokens.size() > 1 && tokens.getFirst().equals("db") && tokens.get(1).equals(".")) {
            extractMongoCollections(tokens, names);
            return names;
        }
        Set<Integer> fromDepths = new HashSet<>();
        Set<Integer> scalarFromDepths = new HashSet<>();
        Map<Integer, Set<String>> commonTables = new HashMap<>();
        boolean select = tokens.stream().anyMatch(token -> token.equalsIgnoreCase("SELECT"));
        int depth = 0;
        boolean indexOrTrigger = false;
        boolean rename = false;
        int statementStart = 0;
        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            String keyword = token.toUpperCase(Locale.ROOT);
            if (token.startsWith("'") && i > 0 && recursion < 8
                && Set.of("EXEC", "EXECUTE", "IMMEDIATE", "DO", "AS").contains(tokens.get(i - 1).toUpperCase(Locale.ROOT))) {
                names.addAll(extractTableNames(token.substring(1, token.length() - 1).replace("''", "'"), recursion + 1));
            }
            if (token.equals("(")) {
                depth++;
                if (i > 0 && Set.of("EXTRACT", "TRIM", "SUBSTRING", "SUBSTR")
                    .contains(tokens.get(i - 1).toUpperCase(Locale.ROOT))) {
                    scalarFromDepths.add(depth);
                }
            } else if (token.equals(")")) {
                fromDepths.remove(depth);
                scalarFromDepths.remove(depth);
                commonTables.remove(depth);
                depth = Math.max(0, depth - 1);
            } else if (token.equals(";")) {
                fromDepths.clear();
                scalarFromDepths.clear();
                commonTables.clear();
                depth = 0;
                indexOrTrigger = false;
                rename = false;
                statementStart = i + 1;
            } else if (keyword.equals("WITH")) {
                commonTables.computeIfAbsent(depth, ignored -> new HashSet<>()).addAll(readCommonTables(tokens, i + 1));
            } else if (keyword.equals("INDEX") || keyword.equals("TRIGGER")) {
                indexOrTrigger = true;
            } else if (keyword.equals("RENAME")) {
                rename = true;
            } else if (TABLE_PREFIXES.contains(keyword) || keyword.equals("ON") && indexOrTrigger
                || keyword.equals("TO") && rename || token.equals(",") && fromDepths.contains(depth)) {
                if (i > 0 && tokens.get(i - 1).equals(".")
                    || keyword.equals("FROM") && scalarFromDepths.contains(depth)
                    || keyword.equals("USING") && indexOrTrigger
                    || (keyword.equals("DESC") || keyword.equals("DESCRIBE")) && i != statementStart) {
                    continue;
                }
                // FOR UPDATE and ON CONFLICT DO UPDATE are clauses, not table references.
                if (keyword.equals("UPDATE") && i > 0 && Set.of("FOR", "DO", "KEY")
                    .contains(tokens.get(i - 1).toUpperCase(Locale.ROOT))) {
                    continue;
                }
                int start = i + 1;
                while (start < tokens.size() && MODIFIERS.contains(tokens.get(start).toUpperCase(Locale.ROOT))) {
                    start++;
                }
                Name name = readName(tokens, start);
                if (name != null) {
                    boolean inlineFunction = name.end() < tokens.size() && tokens.get(name.end()).equals("(")
                        && INLINE_TABLE_FUNCTIONS.contains(name.text().toUpperCase(Locale.ROOT));
                    if (!inlineFunction && commonTables.values().stream().noneMatch(aliases -> aliases.contains(aliasKey(name.text())))) {
                        names.add(name.text());
                    }
                    i = name.end() - 1;
                }
                if (keyword.equals("FROM") || keyword.equals("UPDATE") || keyword.equals("TABLE")) {
                    fromDepths.add(depth);
                }
            } else if (END_FROM.contains(keyword)) {
                fromDepths.remove(depth);
            } else if (select && isName(token) && (i == 0 || !tokens.get(i - 1).equals("."))
                && i + 1 < tokens.size() && tokens.get(i + 1).equals(".")) {
                Name routine = readName(tokens, i);
                if (routine != null && routine.end() < tokens.size() && tokens.get(routine.end()).equals("(")) {
                    names.add(routine.text());
                    i = routine.end() - 1;
                }
            }
        }
        return names;
    }

    @NotNull
    private static Set<String> readCommonTables(@NotNull List<String> tokens, int start) {
        Set<String> aliases = new HashSet<>();
        int i = start;
        if (i < tokens.size() && tokens.get(i).equalsIgnoreCase("RECURSIVE")) {
            i++;
        }
        while (i < tokens.size() && isName(tokens.get(i))) {
            final String alias = tokens.get(i++);
            if (i < tokens.size() && tokens.get(i).equals("(")) {
                i = skipParentheses(tokens, i);
            }
            if (i >= tokens.size() || !tokens.get(i++).equalsIgnoreCase("AS")) {
                break;
            }
            while (i < tokens.size() && Set.of("NOT", "MATERIALIZED").contains(tokens.get(i).toUpperCase(Locale.ROOT))) {
                i++;
            }
            if (i >= tokens.size() || !tokens.get(i).equals("(")) {
                break;
            }
            aliases.add(aliasKey(alias));
            i = skipParentheses(tokens, i);
            if (i >= tokens.size() || !tokens.get(i++).equals(",")) {
                break;
            }
        }
        return aliases;
    }

    @NotNull
    private static String aliasKey(@NotNull String name) {
        return name.startsWith("\"") || name.startsWith("`") || name.startsWith("[") ? name : name.toUpperCase(Locale.ROOT);
    }

    private static int skipParentheses(@NotNull List<String> tokens, int start) {
        int nesting = 0;
        for (int i = start; i < tokens.size(); i++) {
            if (tokens.get(i).equals("(")) {
                nesting++;
            } else if (tokens.get(i).equals(")") && --nesting == 0) {
                return i + 1;
            }
        }
        return tokens.size();
    }

    @Nullable
    private static Name readName(@NotNull List<String> tokens, int start) {
        if (start >= tokens.size() || !isName(tokens.get(start))) {
            return null;
        }
        StringBuilder name = new StringBuilder(tokens.get(start));
        int end = start + 1;
        while (end < tokens.size() && tokens.get(end).equals(".")) {
            // Preserve SQL Server's omitted schema in database..table.
            name.append('.');
            end++;
            if (end < tokens.size() && tokens.get(end).equals(".")) {
                name.append('.');
                end++;
            }
            if (end >= tokens.size() || !isName(tokens.get(end))) {
                return null;
            }
            name.append(tokens.get(end++));
        }
        return new Name(name.toString(), end);
    }

    private static boolean isName(@NotNull String token) {
        if (token.startsWith("\"") || token.startsWith("`") || token.startsWith("[")) {
            return true;
        }
        String keyword = token.toUpperCase(Locale.ROOT);
        return !token.startsWith("@") && !token.startsWith("$") && WORD.matcher(token).matches()
            && !TABLE_PREFIXES.contains(keyword) && !END_FROM.contains(keyword)
            && !MODIFIERS.contains(keyword) && !NOT_NAMES.contains(keyword);
    }

    private static void extractMongoCollections(@NotNull List<String> tokens, @NotNull Set<String> names) {
        for (int i = 0; i + 3 < tokens.size(); i++) {
            if (!tokens.get(i).equals("db") || !tokens.get(i + 1).equals(".")) {
                continue;
            }
            int member = i + 2;
            String database = "";
            if (tokens.get(member).equals("getSiblingDB") && member + 5 < tokens.size()
                && tokens.get(member + 1).equals("(") && tokens.get(member + 3).equals(")")
                && tokens.get(member + 4).equals(".")) {
                database = quotedStringIdentifier(tokens.get(member + 2));
                if (database == null) {
                    continue;
                }
                database += ".";
                member += 5;
            }
            String collection = tokens.get(member);
            if (collection.equals("getCollection") && member + 3 < tokens.size()
                && tokens.get(member + 1).equals("(") && tokens.get(member + 3).equals(")")) {
                String identifier = quotedStringIdentifier(tokens.get(member + 2));
                if (identifier != null) {
                    names.add(database + identifier);
                }
            } else if (isName(collection) && member + 2 < tokens.size() && tokens.get(member + 1).equals(".")) {
                names.add(database + collection);
            }
            boolean command = collection.equals("runCommand");
            boolean aggregation = member + 2 < tokens.size() && tokens.get(member + 2).equals("aggregate")
                || member + 5 < tokens.size() && tokens.get(member + 5).equals("aggregate");
            List<String> owners = new ArrayList<>();
            // Only command arguments and aggregation-stage references name collections. Filter fields do not.
            for (int keyIndex = member; keyIndex + 2 < tokens.size(); keyIndex++) {
                String key = tokens.get(keyIndex);
                if (key.equals(";") || key.equals("db") && tokens.get(keyIndex + 1).equals(".")) {
                    break;
                }
                if (key.equals("{")) {
                    owners.add(keyIndex > 1 && tokens.get(keyIndex - 1).equals(":")
                        ? mongoKey(tokens.get(keyIndex - 2)) : owners.isEmpty() && command ? "$command" : "");
                    continue;
                }
                if (key.equals("}")) {
                    if (!owners.isEmpty()) {
                        owners.removeLast();
                    }
                    continue;
                }
                key = mongoKey(key);
                String owner = owners.isEmpty() ? "" : owners.getLast();
                boolean commandCollection = owner.equals("$command") && Set.of("find", "aggregate", "count", "distinct",
                    "insert", "update", "delete", "findAndModify").contains(key);
                if (commandCollection && key.equals("aggregate")) {
                    aggregation = true;
                }
                boolean stageCollection = aggregation && (
                    Set.of("$lookup", "$graphLookup").contains(owner) && key.equals("from")
                        || owner.equals("$unionWith") && key.equals("coll")
                        || owner.equals("$merge") && key.equals("into")
                        || owner.isEmpty() && Set.of("$out", "$merge", "$unionWith").contains(key));
                if (tokens.get(keyIndex + 1).equals(":") && (commandCollection || stageCollection)) {
                    String identifier = quotedStringIdentifier(tokens.get(keyIndex + 2));
                    if (identifier != null) {
                        names.add(database + identifier);
                    }
                }
            }
        }
    }

    @NotNull
    private static String mongoKey(@NotNull String token) {
        return token.startsWith("\"") || token.startsWith("'") ? token.substring(1, token.length() - 1) : token;
    }

    @Nullable
    private static String quotedStringIdentifier(@NotNull String token) {
        if (token.length() < 2 || !(token.startsWith("'") || token.startsWith("\"")) || token.contains("\\")) {
            return null;
        }
        String quote = token.substring(0, 1);
        String name = token.substring(1, token.length() - 1).replace(quote + quote, quote);
        return "\"" + name.replace("\"", "\"\"") + "\"";
    }

    @NotNull
    private static List<String> tokenize(@NotNull String sql) {
        List<String> tokens = new ArrayList<>();
        Matcher word = WORD.matcher(sql);
        Matcher dollarQuote = DOLLAR_QUOTE.matcher(sql);
        for (int i = 0; i < sql.length();) {
            char c = sql.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
            } else if (sql.startsWith("--", i) || sql.startsWith("//", i)) {
                while (i < sql.length() && sql.charAt(i) != '\n' && sql.charAt(i) != '\r') {
                    i++;
                }
            } else if (sql.startsWith("/*!", i) || sql.startsWith("/*M!", i)) {
                // MySQL/MariaDB executable comments contain SQL, not inert comment text.
                i += sql.startsWith("/*!", i) ? 3 : 4;
                while (i < sql.length() && Character.isDigit(sql.charAt(i))) {
                    i++;
                }
            } else if (sql.startsWith("/*", i)) {
                int nesting = 1;
                i += 2;
                while (i < sql.length() && nesting > 0) {
                    if (sql.startsWith("/*", i)) {
                        nesting++;
                        i += 2;
                    } else if (sql.startsWith("*/", i)) {
                        nesting--;
                        i += 2;
                    } else {
                        i++;
                    }
                }
            } else if (c == '$' && dollarQuote.region(i, sql.length()).lookingAt()) {
                String delimiter = dollarQuote.group();
                int end = sql.indexOf(delimiter, dollarQuote.end());
                if (end < 0) {
                    break;
                }
                tokens.add("'" + sql.substring(dollarQuote.end(), end).replace("'", "''") + "'");
                i = end + delimiter.length();
            } else if ((c == 'q' || c == 'Q') && i + 2 < sql.length() && sql.charAt(i + 1) == '\'') {
                char delimiter = sql.charAt(i + 2);
                char close = switch (delimiter) {
                    case '[' -> ']';
                    case '(' -> ')';
                    case '{' -> '}';
                    case '<' -> '>';
                    default -> delimiter;
                };
                int end = sql.indexOf(close + "'", i + 3);
                if (end < 0) {
                    break;
                }
                tokens.add("'" + sql.substring(i + 3, end).replace("'", "''") + "'");
                i = end + 2;
            } else if (c == '\'' || c == '"' || c == '`' || c == '[' && !isDocumentArray(sql, i + 1)) {
                int end = quotedEnd(sql, i, c == '[' ? ']' : c);
                if (end < 0) {
                    break; // Do not reinterpret an unterminated literal as SQL.
                }
                tokens.add(sql.substring(i, end));
                i = end;
            } else if (word.region(i, sql.length()).lookingAt()) {
                tokens.add(word.group());
                i = word.end();
            } else {
                tokens.add(String.valueOf(c));
                i++;
            }
        }
        return tokens;
    }

    private static boolean isDocumentArray(@NotNull String text, int start) {
        while (start < text.length() && Character.isWhitespace(text.charAt(start))) {
            start++;
        }
        return start < text.length() && text.charAt(start) == '{';
    }

    private static int quotedEnd(@NotNull String sql, int start, char quote) {
        for (int i = start + 1; i < sql.length(); i++) {
            if (sql.charAt(i) == '\\' && quote != ']') {
                i++;
            } else if (sql.charAt(i) == quote) {
                if (i + 1 < sql.length() && sql.charAt(i + 1) == quote) {
                    i++;
                } else {
                    return i + 1;
                }
            }
        }
        return -1;
    }

    private record Name(@NotNull String text, int end) {
    }
}
