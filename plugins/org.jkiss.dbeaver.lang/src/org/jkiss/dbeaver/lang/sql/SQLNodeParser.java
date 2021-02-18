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
package org.jkiss.dbeaver.lang.sql;

import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.lang.*;
import org.jkiss.dbeaver.lang.base.BaseNodeParser;
import org.jkiss.dbeaver.lang.base.SCMEKeyword;
import org.jkiss.dbeaver.lang.parser.KeywordRule;
import org.jkiss.dbeaver.lang.sql.model.SQLStatementAbstract;
import org.jkiss.dbeaver.lang.sql.model.SQLStatementSelect;

import java.util.List;

/**
 * Source code node
 */
public class SQLNodeParser extends BaseNodeParser {
    @Override
    protected void addRules(List<IRule> rules) {
        rules.add(new KeywordRule(SQLKeyword.values()));
        super.addRules(rules);
    }

    @NotNull
    @Override
    public SCMNode parseNode(@NotNull SCMCompositeNode container, @NotNull IToken token, @NotNull SCMSourceScanner scanner) {
        if (token instanceof SCMKeywordToken) {
            // Keyword or identifier
            SCMKeyword keyword = ((SCMKeywordToken)token).getData();
            if (keyword instanceof SQLKeyword) {
                SQLStatementAbstract statement = null;
                switch ((SQLKeyword)keyword) {
                    case SELECT:
                        statement = new SQLStatementSelect(container);
                        break;
                    case INSERT:
                    case UPDATE:
                    case DELETE:
                        break;
                    default:
                        // Unexpected keyword
                        break;
                }
                if (statement != null) {
                    statement.addChild(new SCMEKeyword(statement, scanner));
                    return statement;
                }
            } else {
                // Unknown keyword type
            }

        }
        return super.parseNode(container, token, scanner);
    }
}
