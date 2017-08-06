/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.mysql.model;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.swt.SWT;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCSQLDialect;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.runtime.sql.SQLRuleProvider;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.syntax.tokens.SQLControlToken;
import org.jkiss.utils.ArrayUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
* MySQL dialect
*/
class MySQLDialect extends JDBCSQLDialect implements SQLRuleProvider {

    public static final String[] MYSQL_NON_TRANSACTIONAL_KEYWORDS = ArrayUtils.concatArrays(
        BasicSQLDialect.NON_TRANSACTIONAL_KEYWORDS,
        new String[]{
            "USE", "SHOW",
            "CREATE", "ALTER", "DROP",
            "EXPLAIN", "DESCRIBE", "DESC" }
    );

    public static final String[] ADVANCED_KEYWORDS = {
            "DATABASES",
            "COLUMNS",
    };

    public static final String[][] MYSQL_QUOTE_STRINGS = {
            {"`", "`"},
            {"\"", "\""},
    };

    public MySQLDialect() {
        super("MySQL");
    }

    public void initDriverSettings(JDBCDataSource dataSource, JDBCDatabaseMetaData metaData) {
        super.initDriverSettings(dataSource, metaData);
        //addSQLKeyword("STATISTICS");
        Collections.addAll(tableQueryWords, "EXPLAIN", "DESCRIBE", "DESC");
        addFunctions(Arrays.asList("SLEEP"));

        for (String kw : ADVANCED_KEYWORDS) {
            addSQLKeyword(kw);
        }
        removeSQLKeyword("SOURCE");
    }

    @Nullable
    @Override
    public String[][] getIdentifierQuoteStrings() {
        return MYSQL_QUOTE_STRINGS;
    }

    @Nullable
    @Override
    public String getScriptDelimiterRedefiner() {
        return "DELIMITER";
    }

    @NotNull
    @Override
    public String escapeString(String string) {
        return string.replace("'", "''").replace("\\", "\\\\");
    }

    @NotNull
    @Override
    public String unEscapeString(String string) {
        return string.replace("''", "'").replace("\\\\", "\\");
    }

    @NotNull
    @Override
    public MultiValueInsertMode getMultiValueInsertMode() {
        return MultiValueInsertMode.GROUP_ROWS;
    }

    @Override
    public boolean supportsAliasInSelect() {
        return true;
    }

    @Override
    public boolean supportsCommentQuery() {
        return true;
    }

    @Override
    public String[] getSingleLineComments() {
        return new String[] { "-- ", "#" };
    }

    @Override
    public String getTestSQL() {
        return "SELECT 1";
    }

    @NotNull
    protected String[] getNonTransactionKeywords() {
        return MYSQL_NON_TRANSACTIONAL_KEYWORDS;
    }

    @Override
    public void extendRules(@NotNull List<IRule> rules, @NotNull RulePosition position) {
        if (position == RulePosition.CONTROL) {
            final SQLControlToken sourceToken = new SQLControlToken(
                    new TextAttribute(UIUtils.getGlobalColor(SQLConstants.CONFIG_COLOR_COMMAND), null, SWT.BOLD),
                    "mysql.source");

            EndOfLineRule sourceRule = new EndOfLineRule("source", sourceToken); //$NON-NLS-1$
            rules.add(sourceRule);

            EndOfLineRule sourceRule2 = new EndOfLineRule("SOURCE", sourceToken); //$NON-NLS-1$
            rules.add(sourceRule2);
        }
    }
}
