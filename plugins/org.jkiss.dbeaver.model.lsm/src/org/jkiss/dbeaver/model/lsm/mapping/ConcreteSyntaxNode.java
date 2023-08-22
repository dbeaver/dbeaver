/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.lsm.mapping;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConcreteSyntaxNode extends AbstractSyntaxNode {
    private List<AbstractSyntaxNode> children = null;
    private List<AbstractSyntaxNode> readonlyChildren = null;

    public ConcreteSyntaxNode() {
        super();
    }

    public ConcreteSyntaxNode(@NotNull String name) {
        super(name);
    }

    @NotNull
    public List<AbstractSyntaxNode> getChildren() {
        if (children == null) {
            return Collections.emptyList();
        } else {
            if (readonlyChildren == null) {
                readonlyChildren = Collections.unmodifiableList(children);
            }
            return readonlyChildren;
        }
    }

    @NotNull
    public List<AbstractSyntaxNode> getChildren(@Nullable String name) {
        List<AbstractSyntaxNode> result;
        if (this.children == null) {
            result = Collections.emptyList();
        } else {
            result = new ArrayList<>(this.children.size());
            for (AbstractSyntaxNode subnode : this.children) {
                if ((name == null && subnode.getName() == null) || (name != null && name.equals(subnode.getName()))) {
                    result.add(subnode);
                }
            }
        }
        return result;
    }

    @NotNull
    public <T extends AbstractSyntaxNode> List<T> getChildren(@NotNull Class<T> subnodeType) {
        List<T> result;
        if (this.children == null) {
            result = Collections.emptyList();
        } else {
            result = new ArrayList<>(this.children.size());
            for (AbstractSyntaxNode subnode : this.children) {
                if (subnodeType.isAssignableFrom(subnode.getClass())) {
                    @SuppressWarnings("unchecked")
                    T concreteSubnode = (T) subnode;
                    result.add(concreteSubnode);
                }
            }
        }
        return result;
    }

    void addChild(@NotNull AbstractSyntaxNode node) {
        if (this.children == null) {
            this.children = new ArrayList<>();
        }
        this.children.add(node);
    }
}
