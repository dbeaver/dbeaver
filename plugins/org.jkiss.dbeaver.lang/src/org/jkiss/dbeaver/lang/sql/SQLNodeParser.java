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

import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.ITokenScanner;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.lang.SCMNode;
import org.jkiss.dbeaver.lang.SCMNodeParser;
import org.jkiss.dbeaver.lang.SCMToken;

/**
 * Source code node
 */
public class SQLNodeParser implements SCMNodeParser {


    @Nullable
    @Override
    public SCMNode parseNode(@NotNull IToken token, @NotNull ITokenScanner scanner) {
        if (token.isEOF()) {
            return null;
        }
        Object data = token.getData();
        if (data instanceof SCMToken) {
            switch ((SCMToken) data) {
                case KEYWORD:
                    // Start of statement
                    break;
                case IDENTIFIER:
            }
        }
        return null;
    }
}
