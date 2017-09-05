/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016 Karl Griesser (fullref@gmail.com)
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
package org.jkiss.dbeaver.ext.exasol;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.swt.SWT;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.exasol.model.ExasolDataSource;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCSQLDialect;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.runtime.sql.SQLRuleProvider;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.syntax.rules.SQLFullLineRule;
import org.jkiss.dbeaver.ui.editors.sql.syntax.tokens.SQLControlToken;

import java.sql.SQLException;
import java.util.List;

public class ExasolSQLDialect extends JDBCSQLDialect implements SQLRuleProvider {

    private static final Log LOG = Log.getLog(ExasolDataSource.class);
    
    //Exasol does not support prepareCall
    public static final String[] EXEC_KEYWORDS = new String[]{};


    public ExasolSQLDialect() {
        super("Exasol");
    }

    public void initDriverSettings(JDBCDataSource dataSource, JDBCDatabaseMetaData metaData) {
        super.initDriverSettings(dataSource, metaData);
        try {
            for (String kw : metaData.getSQLKeywords().split(",")) {
                this.addSQLKeyword(kw);
            }
        } catch (SQLException e) {
            LOG.warn("Could not retrieve reserved keyword list from Exasol dictionary");
        }

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

    @NotNull
    @Override
    public String[] getExecuteKeywords() {
        return new String[]{};
    }

    @Override
    public void extendRules(@NotNull List<IRule> rules, @NotNull SQLRuleProvider.RulePosition position) {
        if (position == SQLRuleProvider.RulePosition.CONTROL) {
            final SQLControlToken defineToken = new SQLControlToken(
                    new TextAttribute(UIUtils.getGlobalColor(SQLConstants.CONFIG_COLOR_COMMAND), null, SWT.BOLD),
                    "exasol.define");

            SQLFullLineRule defineRule = new SQLFullLineRule("define", defineToken); //$NON-NLS-1$
            rules.add(defineRule);

            SQLFullLineRule defineRule2 = new SQLFullLineRule("DEFINE", defineToken); //$NON-NLS-1$
            rules.add(defineRule2);
        }
    }

}

