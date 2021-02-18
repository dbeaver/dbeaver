/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
