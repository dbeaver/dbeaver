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

import org.jkiss.code.NotNull;

/**
 * Leaf node - linked to particular source code segment
 */
public abstract class SCMElement implements SCMNode {

    @NotNull
    private final SCMCompositeNode parent;
    private final int beginOffset;
    private final int endOffset;

    public SCMElement(@NotNull SCMCompositeNode parent, int beginOffset, int endOffset) {
        this.parent = parent;
        this.beginOffset = beginOffset;
        this.endOffset = endOffset;
    }

    @Override
    public int getBeginOffset() {
        return beginOffset;
    }

    @Override
    public int getEndOffset() {
        return endOffset;
    }

    @NotNull
    @Override
    public SCMNode getParentNode() {
        return parent;
    }

}
