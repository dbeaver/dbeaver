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
package org.jkiss.dbeaver.ext.bigquery.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.generic.model.GenericSQLDialect;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.parser.SQLParserActionKind;
import org.jkiss.dbeaver.model.sql.parser.tokens.predicates.TokenPredicateFactory;
import org.jkiss.dbeaver.model.sql.parser.tokens.predicates.TokenPredicateSet;
import org.jkiss.dbeaver.model.sql.parser.tokens.predicates.TokenPredicatesCondition;

import java.util.EnumSet;

/**
 * BigQuery SQL dialect
 */
public class BQSQLDialect extends GenericSQLDialect {

    private static final String[] EXEC_KEYWORDS = {"CALL"};

    private static final String[][] BIGQUERY_BEGIN_END_BLOCK = new String[][]{
        {SQLConstants.BLOCK_BEGIN, SQLConstants.BLOCK_END},
        {SQLConstants.KEYWORD_CASE, SQLConstants.BLOCK_END + SQLConstants.KEYWORD_CASE},
        {"IF", SQLConstants.BLOCK_END + " IF"},
        {"LOOP", SQLConstants.BLOCK_END + " LOOP"},
        {"WHILE", SQLConstants.BLOCK_END + " WHILE"},
        {"FOR", SQLConstants.BLOCK_END + " FOR"},
        {"REPEAT", SQLConstants.BLOCK_END + " REPEAT"}
    };

    public BQSQLDialect() {
        super("BigQuery", "google_bigquery");
    }

    @NotNull
    @Override
    public MultiValueInsertMode getDefaultMultiValueInsertMode() {
        return MultiValueInsertMode.GROUP_ROWS;
    }

    @Override
    public char getStringEscapeCharacter() {
        return '\\';
    }

    @Override
    public int getCatalogUsage() {
        return USAGE_ALL;
    }

    @Override
    public int getSchemaUsage() {
        return USAGE_ALL;
    }

    @NotNull
    @Override
    public String[] getExecuteKeywords() {
        return EXEC_KEYWORDS;
    }

    @Override
    public String[][] getBlockBoundStrings() {
        return BIGQUERY_BEGIN_END_BLOCK;
    }

    @Override
    public EnumSet<ProjectionAliasVisibilityScope> getProjectionAliasVisibilityScope() {
        // column alias of is not visible in WHERE, but visible in other projection clauses
        return EnumSet.of(
            ProjectionAliasVisibilityScope.GROUP_BY,
            ProjectionAliasVisibilityScope.HAVING,
            ProjectionAliasVisibilityScope.ORDER_BY
        );
    }

    @Override
    public void initDriverSettings(@NotNull JDBCSession session, @NotNull JDBCDataSource dataSource, @NotNull JDBCDatabaseMetaData metaData) {
        super.initDriverSettings(session, dataSource, metaData);
        super.cachedDialectSkipTokenPredicates = this.makeDialectSkipTokenPredicates(dataSource);
    }

    @NotNull
    @Override
    protected TokenPredicateSet makeDialectSkipTokenPredicatesImpl(@NotNull JDBCDataSource dataSource, @NotNull TokenPredicateFactory tt) {
        return TokenPredicateSet.of(new TokenPredicatesCondition(
            SQLParserActionKind.END_BLOCK,
            tt.sequence(
                tt.alternative(
                    "CREATE", "ALTER", "DROP", "UNDROP", "BEGIN"
                )
            ),
            tt.sequence(
                tt.alternative(
                    "SCHEMA",
                    "CONNECTION",
                    "MODEL",
                    "GRAPH",
                    "FUNCTION",
                    "PROCEDURE",
                    "TABLE",
                    "VIEW",
                    "INDEX",
                    "POLICY",
                    "DATA_POLICY",
                    "COLUMN",
                    "CONSTRAINT",
                    "KEY",
                    "CAPACITY",
                    "RESERVATION",
                    "ASSIGNMENT"
                ),
                "IF", tt.optional("NOT"), "EXISTS"
            )
        ));
    }
}
