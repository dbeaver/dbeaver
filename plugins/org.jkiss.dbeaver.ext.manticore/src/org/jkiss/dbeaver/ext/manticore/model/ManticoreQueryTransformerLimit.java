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
package org.jkiss.dbeaver.ext.manticore.model;

import net.sf.jsqlparser.parser.CCJSqlParser;
import net.sf.jsqlparser.parser.CCJSqlParserConstants;
import net.sf.jsqlparser.parser.ParseException;
import net.sf.jsqlparser.parser.Token;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.PlainSelect;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.sql.QueryTransformerLimit;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.utils.CommonUtils;

/**
 * Expands Manticore's retained result window to include the page requested by the shared LIMIT transformer.
 */
class ManticoreQueryTransformerLimit extends QueryTransformerLimit {
    private static final int DEFAULT_MAX_MATCHES = 1000;

    private long maxMatches;

    @Override
    public void setParameters(@NotNull Object... parameters) {
        super.setParameters(parameters);
        long offset = ((Number) parameters[0]).longValue();
        long length = ((Number) parameters[1]).longValue();
        // Manticore's window is a positive signed integer. Do not overflow or synthesize an unlimited window.
        maxMatches = offset >= 0 && length > 0 && offset <= Integer.MAX_VALUE - length ? offset + length : 0;
    }

    @NotNull
    @Override
    public String transformQueryString(@NotNull SQLQuery query) throws DBCException {
        String text = SQLUtils.removeQueryDelimiter(SQLUtils.getDialectFromDataSource(query.getDataSource()), query.getText());
        int hintOffset = findTrailingHintOffset(text);
        SQLQuery limitQuery = hintOffset < 0 ? query
            : new SQLQuery(query.getDataSource(), text.substring(0, hintOffset).stripTrailing(), query);
        String sql = super.transformQueryString(limitQuery);
        if (sql.equals(limitQuery.getText())) {
            return query.getText();
        }

        // An outer SELECT over a subquery cannot have OPTION. For grouped queries, an explicit max_matches
        // disables automatic group-window sizing and can change aggregate accuracy, so leave that to the server.
        if (maxMatches > DEFAULT_MAX_MATCHES
            && limitQuery.getStatement() instanceof PlainSelect select
            && select.getFromItem() instanceof Table
            && select.getGroupBy() == null
            && CommonUtils.isEmpty(select.getJoins())
            && CommonUtils.isEmpty(select.getWithItemsList())
        ) {
            sql += "\nOPTION max_matches=" + maxMatches;
        }
        // Manticore requires LIMIT and OPTION before its trailing optimizer hint.
        return hintOffset < 0 ? sql : sql + "\n" + text.substring(hintOffset);
    }

    private static int findTrailingHintOffset(@NotNull String sql) throws DBCException {
        if (!sql.contains("/*+")) {
            return -1;
        }
        try {
            // Use the existing lexer to distinguish actual hints from hint-like strings and ordinary comments.
            CCJSqlParser parser = new CCJSqlParser(sql).withBackslashEscapeCharacter(true);
            int hintOffset = -1;
            for (Token token = parser.getNextToken(); ; token = parser.getNextToken()) {
                if (token.kind != CCJSqlParserConstants.EOF && !token.image.equals(";")) {
                    hintOffset = -1;
                    continue;
                }
                // Special comment tokens have no absolute offsets. Locate their exact text backwards from the next token.
                int commentEnd = token.kind == CCJSqlParserConstants.EOF ? sql.length() : token.absoluteBegin - 1;
                for (Token comment = token.specialToken; comment != null; comment = comment.specialToken) {
                    int commentStart = sql.lastIndexOf(comment.image, commentEnd - comment.image.length());
                    if (comment.image.startsWith("/*+")) {
                        hintOffset = commentStart;
                    }
                    commentEnd = commentStart;
                }
                if (token.kind == CCJSqlParserConstants.EOF) {
                    return hintOffset;
                }
            }
        } catch (ParseException e) {
            throw new DBCException("Error locating Manticore optimizer hint", e);
        }
    }
}
