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
package org.jkiss.dbeaver.lang.sql.model;

import org.eclipse.jface.text.rules.IToken;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.lang.SCMCompositeNode;
import org.jkiss.dbeaver.lang.SCMSourceScanner;

/**
 * Source code node
 */
public class SQLStatementSelect extends SQLStatementAbstract {

    public SQLStatementSelect(SCMCompositeNode parent) {
        super(parent);
    }

    @Override
    public String toString() {
        return ">>SELECT ";
    }

    @Nullable
    @Override
    public IToken parseComposite(@NotNull SCMSourceScanner scanner) {
        IToken lastToken = parseSelectExpression(scanner);
        if (lastToken != null && lastToken.isEOF()) {
            return lastToken;
        }
        return null;
    }

    private IToken parseSelectExpression(SCMSourceScanner scanner) {
        for (;;) {
            IToken token = scanner.nextToken();
            if (token.isEOF()) {
                break;
            }
        }
        return null;
    }
}
