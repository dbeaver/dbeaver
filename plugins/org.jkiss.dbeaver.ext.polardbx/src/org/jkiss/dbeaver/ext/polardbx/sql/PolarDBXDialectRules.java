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
package org.jkiss.dbeaver.ext.polardbx.sql;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.sql.parser.rules.SQLMultiWordRule;
import org.jkiss.dbeaver.model.sql.parser.tokens.SQLTokenType;
import org.jkiss.dbeaver.model.text.parser.TPRule;
import org.jkiss.dbeaver.model.text.parser.TPRuleProvider;
import org.jkiss.dbeaver.model.text.parser.TPTokenDefault;

/**
 * PolarDB-X dialect rules to recognize multi-word statements like "CREATE GLOBAL INDEX".
 */
public class PolarDBXDialectRules implements TPRuleProvider {

    @NotNull
    @Override
    public TPRule[] extendRules(@Nullable DBPDataSourceContainer dataSource, @NotNull RulePosition position) {
        if (position == RulePosition.KEYWORDS) {
            final TPTokenDefault keywordToken = new TPTokenDefault(SQLTokenType.T_KEYWORD);
            return new TPRule[] {
                new SQLMultiWordRule(new String[] { "CREATE", "GLOBAL", "INDEX" }, keywordToken),
                new SQLMultiWordRule(new String[] { "DROP", "GLOBAL", "INDEX" }, keywordToken)
            };
        }
        return new TPRule[0];
    }
}