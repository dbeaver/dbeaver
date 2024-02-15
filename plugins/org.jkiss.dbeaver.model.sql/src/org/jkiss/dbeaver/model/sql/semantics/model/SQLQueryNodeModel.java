/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.sql.semantics.model;

import org.antlr.v4.runtime.misc.Interval;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.lsm.mapping.AbstractSyntaxNode;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class SQLQueryNodeModel {

    private final Interval region;
    private List<SQLQueryNodeModel> subnodes; 

    protected SQLQueryNodeModel(@NotNull Interval region) {
        this.region = region;
        this.subnodes = null;
    }

    protected SQLQueryNodeModel(@NotNull Interval region, SQLQueryNodeModel ... subnodes) {
        this.region = region;
        
        if (subnodes == null || subnodes.length == 0) {
            this.subnodes = null;
        } else {
            this.subnodes = Stream.of(subnodes).filter(n -> n != null).collect(Collectors.toCollection(() -> new ArrayList<>(subnodes.length)));
            this.subnodes.sort(Comparator.comparingInt(n -> n.region.a));
        }
    }
    
    protected void registerSubnode(SQLQueryNodeModel subnode) {
        if (this.subnodes == null) {
            this.subnodes = new ArrayList<>();
        }
        int index = AbstractSyntaxNode.binarySearchByKey(this.subnodes, n -> n.region.a, subnode.region.a, Comparator.comparingInt(x -> x));
        if (index < 0) {
            index = ~index;
        }
        this.subnodes.add(index, subnode);
    }

    @NotNull
    public final Interval getInterval() {
        return this.region;
    }

    public final <T, R> R apply(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
        return this.applyImpl(visitor, arg);
    }

    protected abstract <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg);
    
    protected SQLQueryNodeModel findChildNodeContaining(int offset) {
        if (this.subnodes != null) {
            if (this.subnodes.size() == 1) {
                SQLQueryNodeModel node = this.subnodes.get(0);
                return node.region.a <= offset && node.region.b >= offset ? node : null;
            } else {
                int index = AbstractSyntaxNode.binarySearchByKey(this.subnodes, n -> n.region.a, offset, Comparator.comparingInt(x -> x));
                if (index >= 0) {
                    SQLQueryNodeModel node = this.subnodes.get(index);
                    int i = index + 1;
                    while (i < this.subnodes.size()) {
                        SQLQueryNodeModel next = this.subnodes.get(i++);
                        if (next.region.a > offset) {
                            break;
                        } else {
                            node = next;
                            i++;   
                        }
                    }
                    return node;
                } else {
                    for (int i = ~index - 1; i >= 0; i--) {
                        SQLQueryNodeModel node = this.subnodes.get(i);
                        if (node.region.a <= offset && node.region.b >= offset) {
                            return node;
                        } else if (node.region.b < offset) {
                            break;
                        }
                    }
                }
            }
        }
        return null;
    }
    
    public abstract SQLQueryDataContext getDataContext();
}
