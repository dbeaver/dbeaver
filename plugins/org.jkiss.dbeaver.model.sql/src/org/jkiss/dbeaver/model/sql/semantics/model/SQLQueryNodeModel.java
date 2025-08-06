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
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryLexicalScope;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolClass;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolOrigin;
import org.jkiss.dbeaver.model.stm.STMTreeNode;
import org.jkiss.dbeaver.model.stm.STMUtils;
import org.jkiss.dbeaver.utils.ListNode;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents a query model part in the source text. Connects model with syntax tree and text region.
 */
public abstract class SQLQueryNodeModel {

    @NotNull
    private final Interval region;
    @NotNull
    private final STMTreeNode syntaxNode;
    @Nullable
    private List<SQLQueryNodeModel> subnodes; // TODO validate that subnodes are being registered correctly for all nodes
    @Nullable
    private List<SQLQueryLexicalScope> lexicalScopes = null;
    @Nullable
    private SQLQuerySymbolOrigin tailOrigin = null;

    protected SQLQueryNodeModel(@NotNull Interval region, @NotNull STMTreeNode syntaxNode, @Nullable SQLQueryNodeModel... subnodes) {
        this.region = region;
        this.syntaxNode = syntaxNode;

        if (subnodes == null || subnodes.length == 0) {
            this.subnodes = null;
        } else {
            this.subnodes = Stream.of(subnodes)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(() -> new ArrayList<>(subnodes.length)));
            this.subnodes.sort(Comparator.comparingInt(n -> n.region.a));
        }
    }

    @Nullable
    public SQLQuerySymbolClass getAssociatedSymbolClass() {
        return null;
    }

    protected void setTailOrigin(SQLQuerySymbolOrigin tailOrigin) {
        this.tailOrigin = tailOrigin;
    }

    @Nullable
    public SQLQuerySymbolOrigin getTailOrigin() {
        return this.tailOrigin;
    }

    /**
     * Register lexical scopes, if they haven't been registered yet
     */
    public void registerLexicalScope(@NotNull SQLQueryLexicalScope lexicalScope) {
        List<SQLQueryLexicalScope> scopes = this.lexicalScopes;
        if (scopes == null) {
            this.lexicalScopes = scopes = new ArrayList<>();
        }
        scopes.add(lexicalScope);
    }

    /**
     * Returns lexical scope for the text part in the corresponding position
     */
    public SQLQueryLexicalScope findLexicalScope(int position) {
        List<SQLQueryLexicalScope> scopes = this.lexicalScopes;
        if (scopes != null) {
            for (SQLQueryLexicalScope s : scopes) {
                Interval region = s.getInterval();
                if (region.a <= position && region.b >= position) {
                    return s;
                }
            }
        }

        return null;
    }

    protected void registerSubnode(@NotNull SQLQueryNodeModel subnode) {
        this.subnodes = STMUtils.orderedInsert(this.subnodes, n -> n.region.a, subnode, Comparator.comparingInt(x -> x));
    }

    @NotNull
    public final Interval getInterval() {
        return this.region;
    }

    @NotNull
    public final STMTreeNode getSyntaxNode() {
        return this.syntaxNode;
    }

    /**
     * Apply the visitor
     */
    public final <T, R> R apply(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
        return this.applyImpl(visitor, arg);
    }

    protected abstract <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, T arg);

    protected SQLQueryNodeModel findChildNodeContaining(int position) { // TODO check it
        if (this.subnodes != null) {
            if (this.subnodes.size() == 1) {
                SQLQueryNodeModel node = this.subnodes.get(0);
                return node.region.a <= position && node.region.b >= position - 1 ? node : null;
            } else {
                int index = STMUtils.binarySearchByKey(this.subnodes, n -> n.region.a, position, Comparator.comparingInt(x -> x));
                if (index >= 0) {
                    SQLQueryNodeModel node = this.subnodes.get(index);
                    int i = index + 1;
                    while (i < this.subnodes.size()) {
                        SQLQueryNodeModel next = this.subnodes.get(i++);
                        if (next.region.a > position - 1) {
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
                        if (node.region.a <= position && node.region.b >= position - 1) {
                            return node;
                        } else if (node.region.b < position) {
                            break;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * The query model node having extra control over its children traverse handling
     */
    public interface NodeSubtreeTraverseControl<N extends SQLQueryNodeModel, C>  {
        /**
         * Handle only the first child immediately, queue others until the first traverse is finished
         */
        default boolean delayRestChildren() {
            return false;
        }

        /**
         * Returns "logical" children of the current node, not necessarily actual children
         */
        @Nullable
        default List<SQLQueryNodeModel> getChildren() {
            return null;
        }

        /**
         * Returns true when non-default query context should be used for the specified child subtree
         */
        default boolean overridesContextForChild(@NotNull N child) {
            return false;
        }

        /**
         * Returns query context for the specified child subtree
         */
        @Nullable
        default C getContextForChild(@NotNull N child, @Nullable C defaultContext) {
            return defaultContext;
        }
    }

    private record NodeExtraContext<N extends SQLQueryNodeModel, C>(
        @NotNull NodeSubtreeTraverseControl<N, C> provider,
        @NotNull N key
    ) {
    }

    private record NodeEntry<N extends SQLQueryNodeModel, C>(
        @Nullable NodeExtraContext<N, C> context,
        @NotNull N node
    ){
    }

    protected static <N extends SQLQueryNodeModel, C> void traverseSubtreeSmart(
        @NotNull N subroot,
        @NotNull Class<N> childrenType,
        @Nullable C context,
        @NotNull BiConsumer<N, C> action,
        @NotNull BooleanSupplier cancellationChecker
    ) {
        Set<SQLQueryNodeModel> queued = new HashSet<>();
        queued.add(subroot);
        ListNode<NodeEntry<N, C>> queue = ListNode.of(new NodeEntry<>(null, subroot));

        while (queue != null && !cancellationChecker.getAsBoolean()) {
            ListNode<NodeEntry<N, C>>  stack = ListNode.of(queue.data);
            queue = queue.next;
            while (stack != null) {
                if (stack.data != null) {  // first time handling node
                    NodeEntry<N, C> entry = stack.data;
                    N node = entry.node;
                    List<SQLQueryNodeModel> subnodes = ((SQLQueryNodeModel) node).subnodes;
                    if (subnodes != null) { // children presented, push and handle them at first
                        stack = ListNode.push(stack, null); // push null to separate parent-to-handle from its already processed children
                        boolean delayChildren;
                        List<SQLQueryNodeModel> children;
                        NodeSubtreeTraverseControl<N, C> localContextProvider;
                        if (node instanceof NodeSubtreeTraverseControl<?, ?> c) {
                            //noinspection unchecked
                            localContextProvider = (NodeSubtreeTraverseControl<N, C>) c;
                            delayChildren = c.delayRestChildren();
                            children = c.getChildren();
                            if (children == null) {
                                children = subnodes;
                            }
                        } else {
                            localContextProvider = null;
                            delayChildren = false;
                            children = subnodes;
                        }
                        if (!delayChildren) {
                            children = new ArrayList<>(children);
                            Collections.reverse(children);
                        }
                        int index = 0;
                        for (SQLQueryNodeModel childNode : children) {
                            if (childrenType.isInstance(childNode)) {
                                //noinspection unchecked
                                N child = (N) childNode;
                                NodeExtraContext<N, C> extraContext = localContextProvider != null && localContextProvider.overridesContextForChild(child)
                                    ? new NodeExtraContext<N, C>(localContextProvider, child)
                                    : entry.context;
                                NodeEntry<N, C> childEntry = new NodeEntry<>(extraContext, child);
                                if (delayChildren) {
                                    if (index == 0) {
                                        stack = ListNode.push(stack, childEntry);
                                    } else {
                                        if (queued.add(child)) {
                                            queue = ListNode.push(queue, childEntry);
                                        }
                                    }
                                } else {
                                    stack = ListNode.push(stack, childEntry);
                                }
                            }
                            index++;
                        }
                    } else { // no children, handle immediately
                        applyActionForNode(stack.data, context, action);
                        stack = stack.next;
                    }
                } else { // children already handled, handle the node
                    stack = stack.next;
                    applyActionForNode(stack.data, context, action);
                    stack = stack.next;
                }
            }
        }
    }

    private static <N extends SQLQueryNodeModel, C> void applyActionForNode(
        @NotNull NodeEntry<N, C> entry,
        @Nullable C context,
        @NotNull BiConsumer<N, C> action
    ) {
        C currContext = entry.context == null ? context : entry.context.provider.getContextForChild(entry.context.key, context);
        action.accept(entry.node, currContext);
    }


    /**
     * Just traverse the tree to call action on each node
     */
    protected static <N extends SQLQueryNodeModel, C> void traverseSubtreeSimple(
        @NotNull N subroot,
        @NotNull Class<N> childrenType,
        @NotNull Consumer<N> action,
        @NotNull BooleanSupplier cancellationChecker
    ) {
        ListNode<N> stack = ListNode.of(subroot);
        while (stack != null && !cancellationChecker.getAsBoolean()) {
            if (stack.data != null) {  // first time handling node
                SQLQueryNodeModel node = stack.data;
                if (node.subnodes != null) { // children presented, push and handle them at first
                    stack = ListNode.push(stack, null);
                    for (SQLQueryNodeModel child : node.subnodes) {
                        if (childrenType.isInstance(child)) {
                            //noinspection unchecked
                            stack = ListNode.push(stack, (N) child);
                        }
                    }
                } else { // no children, handle immediately
                    action.accept(stack.data);
                    stack = stack.next;
                }
            } else { // children already handled, handle the node
                stack = stack.next;
                action.accept(stack.data);
                stack = stack.next;
            }
        }
    }
}
