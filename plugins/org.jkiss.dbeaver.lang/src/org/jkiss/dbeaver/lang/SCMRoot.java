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
package org.jkiss.dbeaver.lang;

import org.eclipse.jface.text.rules.IToken;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

/**
 * Source code node
 */
public class SCMRoot extends SCMGroupNode {

    private final SCMSourceParser parser;
    private final SCMSourceScanner scanner;

    public SCMRoot(SCMSourceParser parser, SCMSourceScanner scanner) {
        super(null);
        this.parser = parser;
        this.scanner = scanner;
    }

    @NotNull
    @Override
    public SCMSourceText getSource() {
        return scanner.getSource();
    }

    @Nullable
    @Override
    public IToken parseComposite(@NotNull SCMSourceScanner scanner) {

        for (; ; ) {
            IToken token = scanner.nextToken();
            if (token.isEOF()) {
                break;
            }

            parseToken(this, token);
        }

        return null;
    }

    private void parseToken(SCMCompositeNode container, IToken token) {
        SCMNode node = parser.parseNode(container, token, scanner);
        container.addChild(node);
        if (node instanceof SCMCompositeNode) {
            SCMCompositeNode composite = (SCMCompositeNode) node;
            token = composite.parseComposite(scanner);
            if (token != null) {
                parseToken(container, token);
            }
        }
    }

}
