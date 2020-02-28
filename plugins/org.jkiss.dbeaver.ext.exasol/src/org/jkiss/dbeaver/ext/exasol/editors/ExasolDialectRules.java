/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.exasol.editors;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.sql.parser.rules.SQLFullLineRule;
import org.jkiss.dbeaver.model.sql.parser.tokens.SQLControlToken;
import org.jkiss.dbeaver.model.text.parser.TPRule;
import org.jkiss.dbeaver.model.text.parser.TPRuleProvider;

import java.util.List;

/**
* Exasol dialect rules
*/
class ExasolDialectRules implements TPRuleProvider {

    @Override
    public void extendRules(@Nullable DBPDataSourceContainer dataSource, @NotNull List<TPRule> rules, @NotNull RulePosition position) {
        if (position == TPRuleProvider.RulePosition.CONTROL) {
            final SQLControlToken defineToken = new SQLControlToken("exasol.define");

            SQLFullLineRule defineRule = new SQLFullLineRule("define", defineToken); //$NON-NLS-1$
            rules.add(defineRule);

            SQLFullLineRule defineRule2 = new SQLFullLineRule("DEFINE", defineToken); //$NON-NLS-1$
            rules.add(defineRule2);
        }
    }

}
