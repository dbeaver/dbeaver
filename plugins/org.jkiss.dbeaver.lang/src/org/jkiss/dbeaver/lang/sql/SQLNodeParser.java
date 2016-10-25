/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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
