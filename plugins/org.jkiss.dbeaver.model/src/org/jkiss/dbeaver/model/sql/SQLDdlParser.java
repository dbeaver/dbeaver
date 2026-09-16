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
package org.jkiss.dbeaver.model.sql;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Recognizes common container DDL statements not supported by the semantic parser.
 */
final class SQLDdlParser {
    private SQLDdlParser() {
    }

    @Nullable
    static SQLDdlChange parse(@Nullable SQLDialect dialect, @NotNull String sql) {
        SQLDialect effectiveDialect = dialect == null ? BasicSQLDialect.INSTANCE : dialect;
        Parser parser = new Parser(effectiveDialect, sql);
        String action = parser.readWord();
        if (action == null) {
            return null;
        }
        SQLDdlChange.Operation operation;
        try {
            operation = SQLDdlChange.Operation.valueOf(action.toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException e) {
            return null;
        }
        if (operation == SQLDdlChange.Operation.RENAME) {
            return null;
        }
        if (parser.consumeWord("OR")) {
            if (operation != SQLDdlChange.Operation.CREATE || !parser.consumeWord("REPLACE")) {
                return null;
            }
        }
        String kindName = parser.readWord();
        SQLDdlChange.ObjectKind objectKind;
        try {
            objectKind = SQLDdlChange.ObjectKind.valueOf(kindName == null ? "" : kindName.toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException e) {
            return null;
        }
        if (objectKind != SQLDdlChange.ObjectKind.SCHEMA &&
            objectKind != SQLDdlChange.ObjectKind.DATABASE &&
            objectKind != SQLDdlChange.ObjectKind.CATALOG) {
            return null;
        }
        if (parser.consumeWord("IF")) {
            parser.consumeWord("NOT");
            if (!parser.consumeWord("EXISTS")) {
                return null;
            }
        }
        if (operation == SQLDdlChange.Operation.CREATE && objectKind == SQLDdlChange.ObjectKind.SCHEMA &&
            parser.consumeWord("AUTHORIZATION")) {
            return parser.readIdentifier() == null ? null : new SQLDdlChange(operation, objectKind, List.of());
        }
        List<String> nameParts = parser.readQualifiedIdentifier();
        if (nameParts.isEmpty()) {
            return null;
        }
        if (operation == SQLDdlChange.Operation.ALTER && parser.consumeWord("RENAME")) {
            if (!parser.consumeWord("TO") || parser.readQualifiedIdentifier().isEmpty() || !parser.isAtEnd()) {
                return null;
            }
            operation = SQLDdlChange.Operation.RENAME;
        }
        return new SQLDdlChange(operation, objectKind, nameParts);
    }

    private static final class Parser {
        private final SQLDialect dialect;
        private final String sql;
        private int offset;

        private Parser(@NotNull SQLDialect dialect, @NotNull String sql) {
            this.dialect = dialect;
            this.sql = sql;
        }

        @Nullable
        private String readWord() {
            skipSeparators();
            int start = offset;
            while (offset < sql.length() && (Character.isLetter(sql.charAt(offset)) || sql.charAt(offset) == '_')) {
                offset++;
            }
            return start == offset ? null : sql.substring(start, offset);
        }

        private boolean consumeWord(@NotNull String word) {
            int initialOffset = offset;
            String actual = readWord();
            if (word.equalsIgnoreCase(actual)) {
                return true;
            }
            offset = initialOffset;
            return false;
        }

        @NotNull
        private List<String> readQualifiedIdentifier() {
            List<String> parts = new ArrayList<>();
            String part = readIdentifier();
            if (part == null) {
                return parts;
            }
            parts.add(part);
            while (true) {
                skipSeparators();
                if (offset >= sql.length() || sql.charAt(offset) != dialect.getStructSeparator()) {
                    break;
                }
                offset++;
                part = readIdentifier();
                if (part == null) {
                    return List.of();
                }
                parts.add(part);
            }
            return List.copyOf(parts);
        }

        @Nullable
        private String readIdentifier() {
            skipSeparators();
            if (offset >= sql.length()) {
                return null;
            }
            String[][] quoteStrings = dialect.getIdentifierQuoteStrings();
            if (quoteStrings == null) {
                quoteStrings = BasicSQLDialect.DEFAULT_IDENTIFIER_QUOTES;
            }
            for (String[] quotes : quoteStrings) {
                if (sql.startsWith(quotes[0], offset)) {
                    int start = offset;
                    offset += quotes[0].length();
                    while (offset < sql.length()) {
                        int close = sql.indexOf(quotes[1], offset);
                        if (close < 0) {
                            offset = start;
                            return null;
                        }
                        offset = close + quotes[1].length();
                        if (!sql.startsWith(quotes[1], offset)) {
                            return sql.substring(start, offset);
                        }
                        offset += quotes[1].length();
                    }
                    offset = start;
                    return null;
                }
            }
            int start = offset;
            while (offset < sql.length()) {
                char ch = sql.charAt(offset);
                if (Character.isWhitespace(ch) || ch == ';' || ch == dialect.getStructSeparator() || isCommentStart(offset)) {
                    break;
                }
                offset++;
            }
            return start == offset ? null : sql.substring(start, offset);
        }

        private boolean isAtEnd() {
            skipSeparators();
            if (offset < sql.length() && sql.charAt(offset) == ';') {
                offset++;
                skipSeparators();
            }
            return offset == sql.length();
        }

        private void skipSeparators() {
            while (offset < sql.length()) {
                if (Character.isWhitespace(sql.charAt(offset))) {
                    offset++;
                    continue;
                }
                if (sql.startsWith("/*", offset)) {
                    int end = sql.indexOf("*/", offset + 2);
                    offset = end < 0 ? sql.length() : end + 2;
                    continue;
                }
                String commentPrefix = getLineCommentPrefix(offset);
                if (commentPrefix == null) {
                    return;
                }
                int end = offset + commentPrefix.length();
                while (end < sql.length() && sql.charAt(end) != '\r' && sql.charAt(end) != '\n') {
                    end++;
                }
                offset = end;
            }
        }

        private boolean isCommentStart(int position) {
            return sql.startsWith("/*", position) || getLineCommentPrefix(position) != null;
        }

        @Nullable
        private String getLineCommentPrefix(int position) {
            String[] lineComments = dialect.getSingleLineComments();
            if (lineComments == null) {
                return null;
            }
            for (String prefix : lineComments) {
                if (!prefix.isEmpty() && sql.startsWith(prefix, position)) {
                    return prefix;
                }
            }
            return null;
        }
    }
}
