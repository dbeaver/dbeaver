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
package org.jkiss.dbeaver.ui.editors.sql.semantics.model;

import org.antlr.v4.runtime.misc.Interval;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.lsm.mapping.AbstractSyntaxNode;
import org.jkiss.dbeaver.model.stm.STMTreeNode;
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLQueryLexicalScope;
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLQueryLexicalScopeKind;
import org.jkiss.dbeaver.ui.editors.sql.semantics.completion.SQLQueryCompletionScope;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SQLQueryDataContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class SQLQueryNodeModel {

    private final Interval region;
    private final STMTreeNode syntaxNode;
    private List<SQLQueryNodeModel> subnodes; 
    private List<SQLQueryLexicalScope> scopes = null;

//    protected SQLQueryNodeModel(@NotNull Interval region, SQLQueryNodeModel ... subnodes) {
//        this(region, null, subnodes);
//    }
    
    protected SQLQueryNodeModel(@NotNull Interval region, STMTreeNode syntaxNode, SQLQueryNodeModel ... subnodes) {
        this.region = region;
        this.syntaxNode = syntaxNode;
        
        if (subnodes == null || subnodes.length == 0) {
            this.subnodes = null;
        } else {
            this.subnodes = Stream.of(subnodes).filter(n -> n != null).collect(Collectors.toCollection(() -> new ArrayList<>(subnodes.length)));
            this.subnodes.sort(Comparator.comparingInt(n -> n.region.a));
        }
    }
    
    protected SQLQueryCompletionScope prepareCompletionScope(int delta) {
        return SQLQueryCompletionScope.forKeywordsAt(this, delta);
    }

    protected SQLQueryLexicalScope registerScope(STMTreeNode start, STMTreeNode end, boolean includesStart, boolean includesEnd, SQLQueryLexicalScopeKind kind) {
        SQLQueryLexicalScope scope = new SQLQueryLexicalScope(start, end, includesStart, includesEnd, kind);
        this.scopes = AbstractSyntaxNode.orderedInsert(this.scopes, s -> s.getInterval().a, scope , Comparator.comparingInt(x -> x));
        return scope;
    }

    protected void registerSubnode(SQLQueryNodeModel subnode) {
        this.subnodes = AbstractSyntaxNode.orderedInsert(this.subnodes, n -> n.region.a, subnode, Comparator.comparingInt(x -> x));
    }  

    @NotNull
    public final Interval getInterval() {
        return this.region;
    }
    
    public final STMTreeNode getSyntaxNode() {
        return this.syntaxNode;
    }

    public final <T, R> R apply(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
        return this.applyImpl(visitor, arg);
    }

    protected abstract <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg);
    
    protected SQLQueryNodeModel findChildNodeContaining(int offset) { // TODO check it
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
