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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.SQLConstants;

import java.util.regex.Pattern;

/**
 * A routine definition with a dollar-quoted body, as returned by pg_get_functiondef.
 * Keeps the surrounding DDL intact when editing only the body.
 */
public final class PostgreProcedureSource {
    private static final Pattern DOLLAR_QUOTE = Pattern.compile("\\$(?:[\\p{L}_][\\p{L}\\p{N}_]*)?\\$");

    private final String definition;
    private final int start;
    private final int end;
    private final String delimiter;

    private PostgreProcedureSource(@NotNull String definition, int start, int end, @NotNull String delimiter) {
        this.definition = definition;
        this.start = start;
        this.end = end;
        this.delimiter = delimiter;
    }

    @Nullable
    public static PostgreProcedureSource parse(@NotNull String definition) {
        boolean afterAs = false;
        int parentheses = 0;
        for (int offset = 0; offset < definition.length();) {
            char c = definition.charAt(offset);
            if (Character.isWhitespace(c)) {
                offset++;
            } else if (definition.startsWith(SQLConstants.SL_COMMENT, offset)) {
                while (offset < definition.length() && definition.charAt(offset) != '\n' && definition.charAt(offset) != '\r') {
                    offset++;
                }
            } else if (definition.startsWith(SQLConstants.ML_COMMENT_START, offset)) {
                int depth = 1;
                offset += SQLConstants.ML_COMMENT_START.length();
                while (offset < definition.length() && depth > 0) {
                    if (definition.startsWith(SQLConstants.ML_COMMENT_START, offset)) {
                        depth++;
                        offset += SQLConstants.ML_COMMENT_START.length();
                    } else if (definition.startsWith(SQLConstants.ML_COMMENT_END, offset)) {
                        depth--;
                        offset += SQLConstants.ML_COMMENT_END.length();
                    } else {
                        offset++;
                    }
                }
            } else if (c == '\'' || c == '"') {
                // Quoted names, defaults and settings may contain AS or dollar quote markers.
                boolean escaped = c == '\'' && offset > 0
                    && (definition.charAt(offset - 1) == 'E' || definition.charAt(offset - 1) == 'e');
                offset++;
                while (offset < definition.length()) {
                    char next = definition.charAt(offset++);
                    if (escaped && next == '\\' && offset < definition.length()) {
                        offset++;
                    } else if (next == c) {
                        if (offset < definition.length() && definition.charAt(offset) == c) {
                            offset++;
                        } else {
                            break;
                        }
                    }
                }
                afterAs = false;
            } else if (c == '$') {
                var matcher = DOLLAR_QUOTE.matcher(definition).region(offset, definition.length());
                if (!matcher.lookingAt()) {
                    offset++;
                    afterAs = false;
                    continue;
                }
                String quote = matcher.group();
                int close = definition.indexOf(quote, matcher.end());
                if (close < 0) {
                    return null;
                }
                if (afterAs && parentheses == 0) {
                    return new PostgreProcedureSource(definition, offset, close + quote.length(), quote);
                }
                offset = close + quote.length();
                afterAs = false;
            } else if (Character.isLetter(c) || c == '_') {
                int start = offset++;
                while (offset < definition.length()) {
                    char next = definition.charAt(offset);
                    if (!Character.isLetterOrDigit(next) && next != '_' && next != '$') {
                        break;
                    }
                    offset++;
                }
                afterAs = definition.substring(start, offset).equalsIgnoreCase(SQLConstants.KEYWORD_AS);
            } else {
                if (c == '(') {
                    parentheses++;
                } else if (c == ')') {
                    parentheses--;
                }
                offset++;
                afterAs = false;
            }
        }
        return null;
    }

    @NotNull
    public String getBody() {
        return definition.substring(start + delimiter.length(), end - delimiter.length());
    }

    @NotNull
    public String getDefinition() {
        return definition;
    }

    @NotNull
    public String withBody(@NotNull String body) {
        String quote = delimiter;
        // The edited body can introduce the original delimiter, e.g. through nested dynamic SQL.
        while ((body + quote).indexOf(quote) < body.length()) {
            quote = quote.substring(0, quote.length() - 1) + "_$";
        }
        return definition.substring(0, start) + quote + body + quote + definition.substring(end);
    }
}
