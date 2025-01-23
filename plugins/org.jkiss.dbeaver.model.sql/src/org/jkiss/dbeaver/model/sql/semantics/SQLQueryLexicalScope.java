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
package org.jkiss.dbeaver.model.sql.semantics;

import org.antlr.v4.runtime.misc.Interval;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryMemberAccessEntry;
import org.jkiss.dbeaver.model.stm.STMTreeNode;
import org.jkiss.utils.Pair;

import java.util.*;
import java.util.stream.Stream;

public class SQLQueryLexicalScope {

    private static final Comparator<Pair<SQLQueryLexicalScopeItem, Interval>> scopeItemsComparator = (a , b) -> {
        int rc = Integer.compare(a.getSecond().length(), b.getSecond().length());
        if (rc != 0) {
            return rc;
        }
        rc = Integer.compare(a.getSecond().a, b.getSecond().a);
        if (rc != 0) {
            return -rc;
        }
        rc = Integer.compare(getLexicalItemPriority(a.getFirst()), getLexicalItemPriority(b.getFirst()));
        if (rc != 0) {
            return rc;
        }
        return 0;
    };

    @Nullable
    private SQLQuerySymbolOrigin.DataContextSymbolOrigin symbolsOrigin = null;
    @NotNull
    private final List<SQLQueryLexicalScopeItem> items;
    @NotNull
    private final List<STMTreeNode> syntaxNodes;
    @Nullable
    private Interval interval = null;

    public SQLQueryLexicalScope() {
        this.items = new ArrayList<>();
        this.syntaxNodes = new ArrayList<>();
    }

    public SQLQueryLexicalScope(int capacity) {
        this.items = new ArrayList<>(capacity);
        this.syntaxNodes = new ArrayList<>(capacity);
    }

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
                items.stream().map(x -> { Interval r = x.getSyntaxNode().getRealInterval(); return r.b + r.length(); }),
                syntaxNodes.stream().map(x -> { Interval r = x.getRealInterval(); return r.b + r.length(); })
            ).mapToInt(x -> x).max().orElse(Integer.MAX_VALUE);

            this.interval = Interval.of(a, b);
        }
        
        return this.interval;
    }

    public void setInterval(@NotNull Interval interval) {
        this.interval = interval;
    }

    /**
     * Returns lexical scope context. If it is not set, then use context of the model node, from which the scope was obtained.
     */
    @Nullable
    public SQLQuerySymbolOrigin.DataContextSymbolOrigin getSymbolsOrigin() {
        return this.symbolsOrigin;
    }
    
    public void setSymbolsOrigin(@NotNull SQLQuerySymbolOrigin.DataContextSymbolOrigin symbolsOrigin) {
        this.symbolsOrigin = symbolsOrigin;
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

    @Nullable
    public SQLQueryLexicalScopeItem findNearestItem(int position) {
        ArrayList<Pair<SQLQueryLexicalScopeItem, Interval>> candidates = new ArrayList<>(this.items.size());
        for (SQLQueryLexicalScopeItem item : this.items) {
            Interval interval = item.getSyntaxNode().getRealInterval();
            if (interval.a < position && interval.b + 1 >= position) {
                candidates.add(Pair.of(item, interval));
            }
        }
        if (candidates.isEmpty()) {
            return null;
        } else {
            candidates.sort(scopeItemsComparator);
            return candidates.get(0).getFirst();
        }
    }

    private static int getLexicalItemPriority(@NotNull SQLQueryLexicalScopeItem item) {
        if (item instanceof SQLQueryMemberAccessEntry) {
            return 0;
        } else if (item instanceof SQLQuerySymbolEntry) {
            return 1;
        } else if (item instanceof SQLQueryQualifiedName) {
            return 2;
        } else {
            return Integer.MAX_VALUE;
        }
    }
}