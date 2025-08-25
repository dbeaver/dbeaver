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
import org.jkiss.dbeaver.model.sql.semantics.*;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryResultColumn;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsSourceContext;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModel;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.sql.semantics.model.expressions.SQLQueryValueExpression;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

import java.util.List;
import java.util.function.Supplier;

/**
 * Describes natural join clause
 */
public class SQLQueryRowsNaturalJoinModel extends SQLQueryRowsSetOperationModel
    implements SQLQueryNodeModel.NodeSubtreeTraverseControl<SQLQueryRowsSourceModel, SQLQueryRowsDataContext> {

    @Nullable
    private final SQLQueryValueExpression condition;
    @Nullable
    private final List<SQLQuerySymbolEntry> columnsToJoin;

    @Nullable
    private final SQLQueryLexicalScope rightSourceScope;
    @NotNull
    private final SQLQueryLexicalScope conditionScope;

    private final boolean isLateral;

    @NotNull
    private Supplier<SQLQueryRowsDataContext> relatedRightContextProvider = this::getRowsDataContext;

    public SQLQueryRowsNaturalJoinModel(
        @NotNull Interval range,
        @NotNull STMTreeNode syntaxNode,
        @NotNull SQLQueryRowsSourceModel left,
        @Nullable SQLQueryRowsSourceModel right,
        @Nullable SQLQueryLexicalScope rightSourceScope,
        boolean isLateral,
        @Nullable SQLQueryValueExpression condition,
        @NotNull SQLQueryLexicalScope conditionScope
    ) {
        super(range, syntaxNode, left, right);
        this.rightSourceScope = rightSourceScope;
        this.isLateral = isLateral;
        this.condition = condition;
        this.conditionScope = conditionScope;
        this.columnsToJoin = null;

        if (condition != null) {
            super.registerSubnode(condition);
        }

        if (rightSourceScope != null) {
            this.registerLexicalScope(rightSourceScope);
        }
        this.registerLexicalScope(conditionScope);
    }

    public SQLQueryRowsNaturalJoinModel(
        @NotNull Interval range,
        @NotNull STMTreeNode syntaxNode,
        @NotNull SQLQueryRowsSourceModel left,
        @Nullable SQLQueryRowsSourceModel right,
        @Nullable SQLQueryLexicalScope rightSourceScope,
        boolean isLateral,
        @Nullable List<SQLQuerySymbolEntry> columnsToJoin,
        @NotNull SQLQueryLexicalScope conditionScope
    ) {
        super(range, syntaxNode, left, right);
        this.rightSourceScope = rightSourceScope;
        this.isLateral = isLateral;
        this.condition = null;
        this.conditionScope = conditionScope;
        this.columnsToJoin = columnsToJoin;

        if (rightSourceScope != null) {
            this.registerLexicalScope(rightSourceScope);
        }
        this.registerLexicalScope(conditionScope);
    }

    @Nullable
    public SQLQueryValueExpression getCondition() {
        return condition;
    }

    @Nullable
    public List<SQLQuerySymbolEntry> getColumnsToJoin() {
        return columnsToJoin;
    }

    @NotNull
    @Override
    protected SQLQueryRowsSourceContext resolveRowSourcesImpl(
        @NotNull SQLQueryRowsSourceContext context,
        @NotNull SQLQueryRecognitionContext statistics
    ) {
        SQLQueryRowsSourceContext leftResult = this.left.resolveRowSources(context, statistics);

        SQLQueryRowsSourceContext rightContext = (
            this.isLateral ? leftResult : context
        ).setRelatedContextProvider(this.relatedRightContextProvider);

        SQLQueryRowsSourceContext rightResult = this.right != null
            ? this.right.resolveRowSources(rightContext, statistics)
            : context.resetAsUnresolved();
        SQLQueryRowsSourceContext result = leftResult.combine(rightResult);

        if (this.condition != null) {
            this.condition.resolveRowSources(result, statistics);
        }

        return result;
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

    @NotNull
    @Override
    protected SQLQueryRowsDataContext resolveRowDataImpl(
        @NotNull SQLQueryRowsDataContext context,
        @NotNull SQLQueryRecognitionContext statistics
    ) {
        SQLQueryRowsDataContext combinedContext;
        if (this.right != null) {
            combinedContext = this.left.getRowsDataContext().combineForJoin(this, this.right.getRowsDataContext());
        } else {
            combinedContext = this.left.getRowsDataContext();
            statistics.appendError(this.getSyntaxNode(), "Table to join is not specified");
        }

        var rightSourceOrigin = new SQLQuerySymbolOrigin.RowsSourceRef(
            this.getRowsSources().setRelatedContextProvider(this.relatedRightContextProvider)
        );
        if (this.rightSourceScope != null) {
            this.rightSourceScope.setSymbolsOrigin(rightSourceOrigin);
            this.setTailOrigin(rightSourceOrigin);
        }

        if (this.columnsToJoin != null) {
            var columnNameOrigin = new SQLQuerySymbolOrigin.ColumnNameFromRowsData(combinedContext);
            for (SQLQuerySymbolEntry column : columnsToJoin) {
                if (column.isNotClassified()) {
                    SQLQuerySymbol symbol = column.getSymbol();
                    SQLQueryResultColumn leftColumnDef = this.left.getRowsDataContext()
                        .resolveColumn(statistics.getMonitor(), column.getName());
                    SQLQueryResultColumn rightColumnDef = this.right == null ? null : this.right.getRowsDataContext()
                        .resolveColumn(statistics.getMonitor(), column.getName());
                    if (leftColumnDef != null && rightColumnDef != null) {
                        symbol.setDefinition(column); // TODO multiple definitions per symbol
                        symbol.setSymbolClass(SQLQuerySymbolClass.COLUMN);
                    } else {
                        if (leftColumnDef == null) {
                            statistics.appendError(column, "Column " + column.getName() + " not found on the left of join");
                        } else {
                            statistics.appendError(column, "Column " + column.getName() + " not found on the right of join");
                        }
                        symbol.setSymbolClass(SQLQuerySymbolClass.ERROR);
                    }
                    column.setOrigin(columnNameOrigin);
                }
            }
            this.setTailOrigin(columnNameOrigin);
            this.conditionScope.setSymbolsOrigin(columnNameOrigin);
        } else {
            var conditionOrigin = new SQLQuerySymbolOrigin.RowsDataRef(combinedContext);
            this.conditionScope.setSymbolsOrigin(conditionOrigin);
            if (this.getTailOrigin() == null) {
                this.setTailOrigin(conditionOrigin);
            }

            if (this.condition != null) {
                this.condition.resolveValueRelations(combinedContext, statistics);
            }
        }

        return combinedContext;
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
        return visitor.visitRowsNaturalJoin(this, arg);
    }
}