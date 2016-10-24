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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base composite node implementation
 */
public abstract class SCMGroupNode implements SCMCompositeNode {

    private final SCMGroupNode parent;
    private List<SCMNode> childNodes;

    public SCMGroupNode(SCMGroupNode parent) {
        this.parent = parent;
    }

    @Override
    public int getBeginOffset() {
        SCMNode firstChild = getFirstChild();
        return firstChild == null ? 0 : firstChild.getBeginOffset();
    }

    @Override
    public int getEndOffset() {
        SCMNode lastChild = getLastChild();
        return lastChild == null ? 0 : lastChild.getEndOffset();
    }

    @NotNull
    @Override
    public SCMSourceText getSource() {
        if (parent == null) {
            throw new IllegalStateException("Null parent node");
        }
        return parent.getSource();
    }

    @Nullable
    @Override
    public SCMCompositeNode getParentNode() {
        return parent;
    }

    @Nullable
    @Override
    public SCMNode getPreviousNode() {
        return parent == null ? null : parent.getPreviousChild(this);
    }

    @Nullable
    @Override
    public SCMNode getNextNode() {
        return parent == null ? null : parent.getNextChild(this);
    }

    @NotNull
    @Override
    public List<SCMNode> getChildNodes() {
        return childNodes != null ? childNodes : Collections.<SCMNode>emptyList();
    }

    @Override
    public SCMNode getFirstChild() {
        return childNodes == null || childNodes.isEmpty() ? null : childNodes.get(0);
    }

    @Override
    public SCMNode getLastChild() {
        return childNodes == null || childNodes.isEmpty() ? null : childNodes.get(childNodes.size() - 1);
    }

    public SCMNode getNextChild(SCMNode node) {
        int index = childNodes == null ? -1 : childNodes.indexOf(node);
        return index < 0 || index >= childNodes.size() - 1 ? null : childNodes.get(index + 1);
    }

    public SCMNode getPreviousChild(SCMNode node) {
        int index = childNodes == null ? -1 : childNodes.indexOf(node);
        return index < 0 ? null : childNodes.get(index - 1);
    }

    public void addChild(SCMNode node) {
        if (childNodes == null) {
            childNodes = new ArrayList<>();
        }
        childNodes.add(node);
    }
}
