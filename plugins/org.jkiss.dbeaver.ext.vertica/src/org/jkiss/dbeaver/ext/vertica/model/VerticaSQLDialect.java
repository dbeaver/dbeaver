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
package org.jkiss.dbeaver.ext.vertica.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.generic.model.GenericSQLDialect;
import org.jkiss.dbeaver.model.exec.DBCLogicalOperator;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLExpressionFormatter;

import java.util.Arrays;

public class VerticaSQLDialect extends GenericSQLDialect {

    private static final String[][] VERTICA_BEGIN_END_BLOCK = new String[][]{
            {SQLConstants.BLOCK_BEGIN, SQLConstants.BLOCK_END},
            {SQLConstants.KEYWORD_CASE, SQLConstants.BLOCK_END},
    };

    private static String[] VERTICA_KEYWORDS = new String[]{
            // SELECT * FROM keywords WHERE reserved = 'R'
            "BIT",
            "CACHE",
            "COMMENT",
            "CORRELATION",
            "ENCODED",
            "FLEX",
            "ILIKE",
            "ILIKEB",
            "INTERVALYM",
            "ISNULL",
            "KSAFE",
            "LIKEB",
            "MINUS",
            "MONEY",
            "NCHAR",
            "NOTNULL",
            "NULLSEQUAL",
            "OFFSET",
            "PINNED",
            "PROJECTION",
            "SMALLDATETIME",
            "TEXT",
            "TIMESERIES",
            "TIMEZONE",
            "TINYINT",
            "UUID",
            "VARCHAR2"
    };

    private static String[] VERTICA_FUNCTIONS = new String[]{
            "CURRENT_DATABASE",
            "CURRENT_SCHEMA",
            "DATEDIFF",
            "DATETIME",
            "DECODE"
    };

    public VerticaSQLDialect() {
        super("Vertica", "vertica");
    }

    public void initDriverSettings(JDBCSession session, JDBCDataSource dataSource, JDBCDatabaseMetaData metaData) {
        super.initDriverSettings(session, dataSource, metaData);
        addSQLKeywords(Arrays.asList(VERTICA_KEYWORDS));
        addFunctions(Arrays.asList(VERTICA_FUNCTIONS));
    }

    @Override
    public boolean supportsAliasInSelect() {
        return true;
    }

    public String[][] getIdentifierQuoteStrings() {
        return BasicSQLDialect.DEFAULT_IDENTIFIER_QUOTES;
    }

    @Nullable
    @Override
    public SQLExpressionFormatter getCaseInsensitiveExpressionFormatter(@NotNull DBCLogicalOperator operator) {
        if (operator == DBCLogicalOperator.LIKE) {
            return (left, right) -> left + " ILIKE " + right;
        }
        return super.getCaseInsensitiveExpressionFormatter(operator);
    }

    @Override
    public String[][] getBlockBoundStrings() {
        return VERTICA_BEGIN_END_BLOCK;
    }

    @Override
    public boolean supportsInsertAllDefaultValuesStatement() {
        return true;
    }
}
