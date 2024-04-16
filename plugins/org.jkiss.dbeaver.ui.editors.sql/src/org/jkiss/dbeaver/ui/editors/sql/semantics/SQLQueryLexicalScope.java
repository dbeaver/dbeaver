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
package org.jkiss.dbeaver.ui.editors.sql.semantics;

import org.antlr.v4.runtime.misc.Interval;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.stm.STMTreeNode;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SQLQueryDataContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class SQLQueryLexicalScope {
    @Nullable
    private SQLQueryDataContext context = null;
    @NotNull
    private final List<SQLQueryLexicalScopeItem> items = new ArrayList<>();
    @NotNull
    private final List<STMTreeNode> syntaxNodes = new ArrayList<>();
    @Nullable
    private Interval interval = null;

    /**
     * Returns a text interval for this lexical scope
     */
    @NotNull
    public Interval getInterval() {
        if (this.interval == null) {
    
            int a = Stream.concat(
                items.stream().map(x -> x.getSyntaxNode().getRealInterval().a),
                syntaxNodes.stream().map(x -> x.getRealInterval().a)
            ).mapToInt(x -> x).min().orElse(0);

            int b = Stream.concat(
                items.stream().map(x -> x.getSyntaxNode().getRealInterval().a),
                syntaxNodes.stream().map(x -> x.getRealInterval().a)
            ).mapToInt(x -> x).max().orElse(Integer.MAX_VALUE);

            this.interval = Interval.of(a, b);
        }
        
        return this.interval;
    }

    /**
     * Returns lexical scope context. If it is not set, then use context of the model node, from which the scope was obtained.
     */
    @Nullable
    public SQLQueryDataContext getContext() {
        return this.context;
    }
    
    public void setContext(@Nullable SQLQueryDataContext context) {
        this.context = context;
    }

    /**
     * Register item in the lexical scope
     */
    public void registerItem(@NotNull SQLQueryLexicalScopeItem item) {
        this.items.add(item);
    }

    /**
     * Register syntax node in the lexical scope
     */
    public void registerSyntaxNode(@NotNull STMTreeNode syntaxNode) {
        this.syntaxNodes.add(syntaxNode);
    }

    /**
     * Find lexical scope item in the provided position in the source text
     */
    @Nullable
    public SQLQueryLexicalScopeItem findItem(int position) {
        return this.items.stream()
           .filter(t -> t.getSyntaxNode().getRealInterval().properlyContains(Interval.of(position, position)))
           .min(Comparator.comparingInt(t -> t.getSyntaxNode().getRealInterval().a))
           .orElse(null);
    }
}