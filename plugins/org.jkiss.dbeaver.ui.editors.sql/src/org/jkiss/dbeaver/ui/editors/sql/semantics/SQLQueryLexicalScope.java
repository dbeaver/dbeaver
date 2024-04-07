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
import org.jkiss.dbeaver.model.stm.STMTreeNode;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SQLQueryDataContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class SQLQueryLexicalScope {
    private SQLQueryDataContext context = null;
    private List<SQLQueryLexicalScopeItem> items = new ArrayList<>();
    private List<STMTreeNode> syntaxNodes = new ArrayList<>();
    
    private Interval interval = null;

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

    public SQLQueryDataContext getContext() {
        return this.context; // if it is not set, then use context of the model node, from which the scope was obtained
    }
    
    public void setContext(SQLQueryDataContext context) {
        this.context = context;
    }
    
    public void registerItem(SQLQueryLexicalScopeItem item) {
        this.items.add(item);
    }
    
    public void registerSyntaxNode(STMTreeNode syntaxNode) {
        this.syntaxNodes.add(syntaxNode);
    }
    
    public SQLQueryLexicalScopeItem findItem(int position) {
        return this.items.stream()
           .filter(t -> t.getSyntaxNode().getRealInterval().properlyContains(Interval.of(position, position)))
           .min(Comparator.comparingInt(t -> t.getSyntaxNode().getRealInterval().a))
           .orElse(null);
    }
}