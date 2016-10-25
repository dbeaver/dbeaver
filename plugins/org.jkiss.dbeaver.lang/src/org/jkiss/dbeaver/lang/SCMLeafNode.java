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
import org.jkiss.code.Nullable;

/**
 * Leaf node - linked to particular source code segment
 */
public abstract class SCMLeafNode implements SCMNode {

    @NotNull
    protected final SCMCompositeNode parent;
    protected int beginOffset;
    protected int endOffset;

    public SCMLeafNode(@NotNull SCMCompositeNode parent, @NotNull SCMSourceScanner scanner) {
        this.parent = parent;
        this.beginOffset = scanner.getTokenOffset();
        this.endOffset = this.beginOffset + scanner.getTokenLength();
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
    public SCMCompositeNode getParentNode() {
        return parent;
    }

    @Nullable
    @Override
    public SCMNode getPreviousNode() {
        return parent.getPreviousChild(this);
    }

    @Nullable
    @Override
    public SCMNode getNextNode() {
        return parent.getNextChild(this);
    }

    public String getPlainValue() {
        return parent.getSource().getSegment(beginOffset, endOffset);
    }

    @Override
    public String toString() {
        return getPlainValue();
    }
}
