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
package org.jkiss.dbeaver.lang.base;

import org.eclipse.jface.text.rules.IToken;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.lang.*;

/**
 * Source code node
 */
public class BaseNodeParser implements SCMNodeParser {


    @NotNull
    @Override
    public SCMNode parseNode(@NotNull SCMGroupNode container, @NotNull IToken token, @NotNull SCMSourceScanner scanner) {
        int tokenOffset = scanner.getTokenOffset();
        int tokenLength = scanner.getTokenLength();
        if (token.isWhitespace()) {
            return new SCMEWhitespace(container, tokenOffset, tokenLength);
        }
        Object data = token.getData();
        if (data instanceof SCMToken) {
            switch ((SCMToken) data) {
                case NUMBER:
                    return new SCMENumber(container, tokenOffset, tokenLength);
                case STRING:
                    return new SCMEString(container, tokenOffset, tokenLength);
                case WHITESPACE:
                    return new SCMENumber(container, tokenOffset, tokenLength);
            }
        }
        return new SCMEUndefined(container, tokenOffset, tokenLength);
    }
}
