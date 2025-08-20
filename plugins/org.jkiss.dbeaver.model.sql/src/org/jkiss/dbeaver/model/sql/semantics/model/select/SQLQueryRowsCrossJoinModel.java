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
package org.jkiss.dbeaver.model.sql.semantics.model.select;

import org.antlr.v4.runtime.misc.Interval;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryLexicalScope;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolOrigin;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsSourceContext;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModel;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

/**
 * Describes cross join clause
 */
public class SQLQueryRowsCrossJoinModel extends SQLQueryRowsSetOperationModel
    implements SQLQueryNodeModel.NodeSubtreeTraverseControl<SQLQueryRowsSourceModel, SQLQueryRowsDataContext> {

    private final boolean isLateral;

    @Nullable
    private final SQLQueryLexicalScope rightSourceScope;

    public SQLQueryRowsCrossJoinModel(
        @NotNull Interval range,
        @NotNull STMTreeNode syntaxNode,
        @NotNull SQLQueryRowsSourceModel left,
        @NotNull SQLQueryRowsSourceModel right,
        @Nullable SQLQueryLexicalScope rightSourceScope,
        boolean isLateral
    ) {
        super(range, syntaxNode, left, right);
        this.rightSourceScope = rightSourceScope;
        this.isLateral = isLateral;

        if (rightSourceScope != null) {
            this.registerLexicalScope(rightSourceScope);
        }
    }

    @Override
    protected SQLQueryRowsSourceContext resolveRowSourcesImpl(
        @NotNull SQLQueryRowsSourceContext context,
        @NotNull SQLQueryRecognitionContext statistics
    ) {
        SQLQueryRowsSourceContext left = this.left.resolveRowSources(context, statistics);
        SQLQueryRowsSourceContext right = this.right != null
            ? this.right.resolveRowSources(this.isLateral ? left : context, statistics)
            : context.resetAsUnresolved();
        SQLQueryRowsSourceContext combined = left.combine(right);
        return combined;
    }

    @Override
    public boolean overridesContextForChild(@NotNull SQLQueryRowsSourceModel child) {
        return this.isLateral && child == this.right;
    }

    @Nullable
    @Override
    public SQLQueryRowsDataContext getContextForChild(
        @NotNull SQLQueryRowsSourceModel child,
        @Nullable SQLQueryRowsDataContext defaultContext
    ) {
        return this.isLateral && child == this.right ? this.left.getRowsDataContext() : defaultContext;
    }

    @Override
    protected SQLQueryRowsDataContext resolveRowDataImpl(
        @NotNull SQLQueryRowsDataContext context,
        @NotNull SQLQueryRecognitionContext statistics
    ) {
        var rightSourceOrigin = new SQLQuerySymbolOrigin.RowsSourceRef(this.getRowsSources());
        if (this.rightSourceScope != null) {
            this.rightSourceScope.setSymbolsOrigin(rightSourceOrigin);
            this.setTailOrigin(rightSourceOrigin);
        }

        if (this.right != null) {
            return this.left.getRowsDataContext().combine(this.right.getRowsDataContext());
        } else {
            statistics.appendError(this.getSyntaxNode(), "Table to join is not specified");
            return this.left.getRowsDataContext();
        }
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
        return visitor.visitRowsCrossJoin(this, arg);
    }
}