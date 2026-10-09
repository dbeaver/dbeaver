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
package org.jkiss.dbeaver.ext.firebird.model;

import org.eclipse.jface.text.BadLocationException;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.sql.SQLQueryParameter;
import org.jkiss.dbeaver.model.sql.parser.DefaultSQLQueryParameterParser;
import org.jkiss.dbeaver.model.sql.parser.SQLParserContext;
import org.jkiss.dbeaver.model.sql.parser.SQLQueryParameterParser;

import java.util.List;
import java.util.regex.Pattern;

/** Creates a parser for each Firebird query. */
public class FireBirdQueryParameterParser implements SQLQueryParameterParser {
    private static final Log log = Log.getLog(FireBirdQueryParameterParser.class);
    private static final String KEYWORD_SEPARATOR = "(?:\\s|--[^\\r\\n]*(?:\\r\\n|[\\r\\n])|/\\*.*?\\*/)+";
    // Recognizes EXECUTE BLOCK at the start, ignoring case and leading whitespace/comments.
    // Between EXECUTE and BLOCK accepts whitespace and SQL comments (no nested block comments).
    private static final Pattern BLOCK = Pattern.compile(
        "^(?:\\s|--[^\\r\\n]*|/\\*.*?\\*/)*EXECUTE" + KEYWORD_SEPARATOR + "BLOCK\\b",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern PROCEDURE = Pattern.compile(
        "^(?:\\s|--[^\\r\\n]*|/\\*.*?\\*/)*EXECUTE" + KEYWORD_SEPARATOR + "PROCEDURE\\b",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    @Nullable
    @Override
    public List<SQLQueryParameter> parseParametersAndVariables(@NotNull SQLParserContext context, int offset, int length) {
        return new FirebirdSQLQueryParameterParser(context, offset, length).parseParametersAndVariables();
    }

    private static class FirebirdSQLQueryParameterParser extends DefaultSQLQueryParameterParser {
        private final boolean block;
        private final boolean procedure;

        private FirebirdSQLQueryParameterParser(@NotNull SQLParserContext context, int offset, int length) {
            super(context, offset, length);
            boolean isBlock = false;
            boolean isProcedure = false;
            try {
                String text = context.getDocument().get(queryOffset, queryLength);
                isBlock = BLOCK.matcher(text).find();
                isProcedure = !isBlock && PROCEDURE.matcher(text).find();
            } catch (BadLocationException e) {
                log.warn("Can't examine Firebird statement parameters", e);
            }
            block = isBlock;
            procedure = isProcedure;
        }

        @Nullable
        @Override
        protected SQLQueryParameter parseNamedParameter(@NotNull String name, int offset, int length, int ordinal) {
            return block || procedure ? null :
                super.parseNamedParameter(name, offset, length, ordinal);
        }

        @Nullable
        @Override
        protected SQLQueryParameter parseAnonymousParameter(@NotNull String mark, int offset, int ordinal) {
            if (block && !"?".equals(mark)) {
                return null;
            }
            SQLQueryParameter parameter = super.parseAnonymousParameter(mark, offset, ordinal);
            if (parameter != null && block) {
                parameter.setNativeBinding(true);
            }
            return parameter;
        }

        @Override
        protected boolean isAnonymousParametersEnabled() {
            return block || super.isAnonymousParametersEnabled();
        }

        @Override
        protected char getAnonymousParameterMark() {
            return block ? '?' : super.getAnonymousParameterMark();
        }

        @Override
        protected boolean isAnonymousParameterIgnored() {
            return !block && (procedure || super.isAnonymousParameterIgnored());
        }
    }
}
