/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.semantics.*;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsSourceContext;
import org.jkiss.dbeaver.model.stm.STMTreeNode;
import org.jkiss.dbeaver.model.stm.STMUtils;
import org.jkiss.dbeaver.utils.ListNode;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Query model for recognition
 */
public class SQLQueryModel extends SQLQueryNodeModel {
    @NotNull
    private final Set<SQLQuerySymbolEntry> symbolEntries;
    @Nullable
    private final SQLQueryModelContent queryContent;
    @NotNull
    private final List<SQLQueryLexicalScopeItem> lexicalItems;

    public SQLQueryModel(
        @NotNull STMTreeNode syntaxNode,
        @Nullable SQLQueryModelContent queryContent,
        @NotNull Set<SQLQuerySymbolEntry> symbolEntries,
        @NotNull List<SQLQueryLexicalScopeItem> lexicalItems
    ) {
        super(syntaxNode.getRealInterval(), syntaxNode, queryContent);
        this.queryContent = queryContent;
        this.symbolEntries = symbolEntries;
        this.lexicalItems = lexicalItems;
    }

    @NotNull
    public Collection<SQLQuerySymbolEntry> getAllSymbols() {
        return symbolEntries;
    }

    @Nullable
    public SQLQueryModelContent getQueryModel() {
        return this.queryContent;
    }

    /**
     * Propagate semantics context and establish relations through the query model
     */
    public void resolveRelations(@NotNull SQLQueryRowsSourceContext rowsContext, @NotNull SQLQueryRecognitionContext recognitionContext) {

        if (this.queryContent != null) {
            this.queryContent.resolveObjectAndRowsReferences(rowsContext, recognitionContext);
            this.queryContent.resolveValueRelations(rowsContext.makeEmptyTuple(), recognitionContext);
        }

        int actualTailPosition = this.getSyntaxNode().getRealInterval().b;
        SQLQueryNodeModel tailNode = this.findNodeContaining(actualTailPosition);
        if (tailNode != this) {
            SQLQuerySymbolOrigin tailOrigin = tailNode.getTailOrigin();
            if (tailOrigin == null) {
                SQLQueryLexicalScope tailNodeScope = tailNode.findLexicalScope(actualTailPosition);
                if (tailNodeScope != null) {
                    tailOrigin = tailNodeScope.getSymbolsOrigin();
                }
            }
            if (tailOrigin != null) {
                this.setTailOrigin(tailOrigin);
            }
        }
    }

    /**
     * Returns nested node of the query model for the specified offset in the source text
     */
    @NotNull
    public SQLQueryNodeModel findNodeContaining(int textOffset) {
        SQLQueryNodeModel node = this;
        SQLQueryNodeModel nested = node.findChildNodeContaining(textOffset);
        while (nested != null) {
            node = nested;
            nested = nested.findChildNodeContaining(textOffset);
        }
        return node;
    }

    public record LexicalContextResolutionResult(
        int textOffset,
        SQLQueryLexicalScopeItem lexicalItem,
        SQLQuerySymbolOrigin symbolsOrigin
    ) {
    }

    /**
     * Returns nested node of the query model for the specified offset in the source text
     */
    public LexicalContextResolutionResult findLexicalContext(int textOffset) {
        ListNode<SQLQueryNodeModel> stack = ListNode.of(this);
        { // walk down through the model till the deepest node describing given position
            SQLQueryNodeModel node = this;
            SQLQueryNodeModel nested = node.findChildNodeContaining(textOffset);
            while (nested != null) {
                stack = ListNode.push(stack, nested);
                nested = nested.findChildNodeContaining(textOffset);
            }
        }

        SQLQueryLexicalScopeItem lexicalItem = null;
        SQLQueryLexicalScope scope = null;
        SQLQuerySymbolOrigin deepestTailOrigin = null;

        // walk up till the lexical scope covering given position
        // TODO consider corner-cases with adjacent scopes, maybe better use condition on lexicalItem!=null instead of the scope?
        while (stack != null && (scope == null || deepestTailOrigin == null)) {
            SQLQueryNodeModel node = stack.data;
            if (scope == null) {
                scope = node.findLexicalScope(textOffset);
                if (scope != null) {
                    lexicalItem = scope.findNearestItem(textOffset);
                }
            }
            if (deepestTailOrigin == null && node.getTailOrigin() != null) {
                deepestTailOrigin = node.getTailOrigin();
            }
            stack = stack.next;
        }

        if (lexicalItem == null) {
            // table refs are not registered in lexical scopes properly for now (because rowsets model being build bottom-to-top),
            // so trying to find their components in the global list
            int index = STMUtils.binarySearchByKey(this.lexicalItems, n -> n.getSyntaxNode().getRealInterval().a, textOffset - 1, Comparator.comparingInt(x -> x));
            if (index < 0) {
                index = ~index - 1;
            }
            if (index >= 0) {
                SQLQueryLexicalScopeItem item = lexicalItems.get(index);
                Interval interval = item.getSyntaxNode().getRealInterval();
                if (interval.a < textOffset && interval.b + 1 >= textOffset) {
                    lexicalItem = item;
                }
            }
        }

        SQLQuerySymbolOrigin symbolsOrigin = lexicalItem == null ? null : lexicalItem.getOrigin();
        if (symbolsOrigin == null && textOffset > this.getInterval().b) {
            symbolsOrigin = this.getTailOrigin();
            if (symbolsOrigin == null) {
                symbolsOrigin = deepestTailOrigin;
            }
        }
        if (symbolsOrigin == null && scope != null) {
            symbolsOrigin = scope.getSymbolsOrigin();
        }

        return new LexicalContextResolutionResult(textOffset, lexicalItem, symbolsOrigin);
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
        return visitor.visitSelectionModel(this, arg);
    }
}
