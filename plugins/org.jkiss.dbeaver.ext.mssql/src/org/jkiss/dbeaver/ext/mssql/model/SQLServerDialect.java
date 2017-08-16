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
package org.jkiss.dbeaver.ext.mssql.model;

import org.eclipse.jface.text.rules.IRule;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.generic.model.GenericSQLDialect;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.runtime.sql.SQLRuleProvider;

import java.util.List;

public class SQLServerDialect extends GenericSQLDialect implements SQLRuleProvider {

    public static final String[][] SQLSERVER_QUOTE_STRINGS = {
            {"[", "]"},
            {"\"", "\""},
    };

    public SQLServerDialect() {
        super("SQLServer");
    }

    public void initDriverSettings(JDBCDataSource dataSource, JDBCDatabaseMetaData metaData) {
        super.initDriverSettings(dataSource, metaData);
    }

    public String[][] getIdentifierQuoteStrings() {
        return SQLSERVER_QUOTE_STRINGS;
    }

    @Override
    public void extendRules(@NotNull List<IRule> rules, @NotNull RulePosition position) {
    }
}
