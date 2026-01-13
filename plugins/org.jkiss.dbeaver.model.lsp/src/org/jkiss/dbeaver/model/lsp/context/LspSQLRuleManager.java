/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.lsp.context;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.sql.parser.SQLRuleManager;
import org.jkiss.dbeaver.model.text.parser.TPRule;
import org.jkiss.dbeaver.model.text.parser.TPTokenAbstract;
import org.jkiss.dbeaver.model.text.parser.rules.NewLineRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LspSQLRuleManager extends SQLRuleManager {

    public LspSQLRuleManager(@NotNull SQLSyntaxManager syntaxManager) {
        super(syntaxManager);
    }

    // TODO: A hack to include a newline rule in the list of rules, replace with proper impl after POC
    @NotNull
    @Override
    public TPRule[] getAllRules() {
        List<TPRule> allRules = new ArrayList<>(Arrays.asList(super.getAllRules()));
        allRules.addFirst(new NewLineRule(TPTokenAbstract.NEWLINE));
        return allRules.toArray(new TPRule[0]);
    }
}
