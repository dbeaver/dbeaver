/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.lang;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.lang.base.SCMEWhitespace;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base composite node implementation
 */
public abstract class SCMGroupNode implements SCMCompositeNode {

    private final SCMCompositeNode parent;
    private List<SCMNode> childNodes;

    public SCMGroupNode(SCMCompositeNode parent) {
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

    @Override
    public SCMNode getNextChild(SCMNode node) {
        int index = childNodes == null ? -1 : childNodes.indexOf(node);
        return index < 0 || index >= childNodes.size() - 1 ? null : childNodes.get(index + 1);
    }

    @Override
    public SCMNode getPreviousChild(SCMNode node) {
        int index = childNodes == null ? -1 : childNodes.indexOf(node);
        return index < 0 ? null : childNodes.get(index - 1);
    }

    @Override
    public void addChild(@NotNull SCMNode node) {
        if (childNodes == null) {
            childNodes = new ArrayList<>();
        }
        childNodes.add(node);
    }

    @Override
    public String toString() {
        String typeName = getClass().getSimpleName();
        if (CommonUtils.isEmpty(childNodes)) {
            return typeName;
        }
        StringBuilder str = new StringBuilder();
        str.append("[");
        for (SCMNode child : childNodes) {
            if (child instanceof SCMEWhitespace){
                str.append(child);
            } else {
                str.append("\t").append(child).append("\n");
            }
        }
        str.append("]");
        return str.toString();
    }
}
