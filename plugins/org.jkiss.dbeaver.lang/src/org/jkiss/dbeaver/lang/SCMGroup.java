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

import java.util.ArrayList;
import java.util.List;

/**
 * Base composite node implementation
 */
public abstract class SCMGroup implements SCMCompositeNode {

    private final List<SCMNode> childNodes = new ArrayList<>();

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

    @Override
    public List<SCMNode> getChildNodes() {
        return childNodes;
    }

    @Override
    public SCMNode getFirstChild() {
        return childNodes.isEmpty() ? null : childNodes.get(0);
    }

    @Override
    public SCMNode getLastChild() {
        return childNodes.isEmpty() ? null : childNodes.get(childNodes.size() - 1);
    }

    public SCMNode getNextChild(SCMNode node) {
        int index = childNodes.indexOf(node);
        return index < 0 || index >= childNodes.size() - 1 ? null : childNodes.get(index + 1);
    }

    public SCMNode getPreviousChild(SCMNode node) {
        int index = childNodes.indexOf(node);
        return index < 0 ? null : childNodes.get(index - 1);
    }

    public void addChild(SCMNode node) {
        childNodes.add(node);
    }
}
