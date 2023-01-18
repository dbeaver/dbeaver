/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.snowflake.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.generic.model.GenericSQLDialect;
import org.jkiss.dbeaver.ext.snowflake.SnowflakeConstants;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.parser.rules.SQLDollarQuoteRule;
import org.jkiss.dbeaver.model.sql.parser.rules.SQLMultiWordRule;
import org.jkiss.dbeaver.model.sql.parser.tokens.SQLTokenType;
import org.jkiss.dbeaver.model.text.parser.*;

import java.util.Arrays;
import java.util.List;

public class SnowflakeSQLDialect extends GenericSQLDialect implements TPRuleProvider {

    private static final String[][] SNOWFLAKE_BEGIN_END_BLOCK = new String[][]{
        {SQLConstants.BLOCK_BEGIN, SQLConstants.BLOCK_END},
        {"IF", SQLConstants.BLOCK_END}
    };

    public SnowflakeSQLDialect() {
        super("Snowflake", "snowflake");
    }

    public void initDriverSettings(JDBCSession session, JDBCDataSource dataSource, JDBCDatabaseMetaData metaData) {
        super.initDriverSettings(session, dataSource, metaData);
        addSQLKeywords(
            Arrays.asList(
                "QUALIFY",
                "ILIKE",
                "PACKAGE",
                "PIPE",
                "STAGE",
                "STREAM",
                "TAG",
                "TASK"
            ));
    }

    @Override
    public void extendRules(@Nullable DBPDataSourceContainer dataSource, @NotNull List<TPRule> rules, @NotNull RulePosition position) {
        if (position == RulePosition.INITIAL || position == RulePosition.PARTITION) {
            rules.add(new SQLDollarQuoteRule(
                position == RulePosition.PARTITION,
                false,
                false,
                dataSource == null || dataSource.getPreferenceStore().getBoolean(SnowflakeConstants.PROP_DD_STRING)
            ));
        }
        if (position == RulePosition.KEYWORDS) {
            final TPTokenDefault keywordToken = new TPTokenDefault(SQLTokenType.T_KEYWORD);
            rules.add(new SQLMultiWordRule(new String[]{"BEGIN", "TRANSACTION"}, keywordToken));
            rules.add(new SQLMultiWordRule(new String[]{"IF", "EXISTS"}, keywordToken));
            rules.add(new SQLMultiWordRule(new String[]{"IF", "NOT", "EXISTS"}, keywordToken));
        }
    }

    @Override
    public String[][] getBlockBoundStrings() {
        return SNOWFLAKE_BEGIN_END_BLOCK;
    }

    @NotNull
    @Override
    public String getSearchStringEscape() {
        // Without escaping of wildcards Snowflake reads all metadata directly from database and ignores specified objects names
        // #9875
        return "\\";
    }

    @NotNull
    @Override
    public MultiValueInsertMode getDefaultMultiValueInsertMode() {
        return MultiValueInsertMode.GROUP_ROWS;
    }
}
