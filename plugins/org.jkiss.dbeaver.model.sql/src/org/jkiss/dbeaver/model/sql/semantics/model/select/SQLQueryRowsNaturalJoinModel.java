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
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryResultColumn;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.sql.semantics.model.expressions.SQLQueryValueExpression;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsSourceContext;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

import java.util.List;

/**
 * Describes natural join clause
 */
public class SQLQueryRowsNaturalJoinModel extends SQLQueryRowsSetOperationModel {
    @Nullable
    private final SQLQueryValueExpression condition;
    @Nullable
    private final List<SQLQuerySymbolEntry> columnsToJoin;

    @NotNull
    private final SQLQueryLexicalScope conditionScope;

    private final boolean isLateral;

    public SQLQueryRowsNaturalJoinModel(
        @NotNull Interval range,
        @NotNull STMTreeNode syntaxNode,
        @NotNull SQLQueryRowsSourceModel left,
        @NotNull SQLQueryRowsSourceModel right,
        boolean isLateral,
        @NotNull SQLQueryValueExpression condition,
        @NotNull SQLQueryLexicalScope conditionScope
    ) {
        super(range, syntaxNode, left, right);
        super.registerSubnode(condition);
        this.isLateral = isLateral;
        this.condition = condition;
        this.conditionScope = conditionScope;
        this.columnsToJoin = null;
        
        this.registerLexicalScope(conditionScope);
    }

    public SQLQueryRowsNaturalJoinModel(
        @NotNull Interval range,
        @NotNull STMTreeNode syntaxNode,
        @NotNull SQLQueryRowsSourceModel left,
        @NotNull SQLQueryRowsSourceModel right,
        boolean isLateral,
        @Nullable List<SQLQuerySymbolEntry> columnsToJoin,
        @NotNull SQLQueryLexicalScope conditionScope
    ) {
        super(range, syntaxNode, left, right);
        this.isLateral = isLateral;
        this.condition = null;
        this.conditionScope = conditionScope;
        this.columnsToJoin = columnsToJoin;

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
    protected SQLQueryDataContext propagateContextImpl(
        @NotNull SQLQueryDataContext context,
        @NotNull SQLQueryRecognitionContext statistics
    ) {
        SQLQueryDataContext left = this.left.propagateContext(context, statistics);
        SQLQueryDataContext right = this.right.propagateContext(this.isLateral ? left : context, statistics);
        SQLQueryDataContext combinedContext = left.combineForJoin(right);

        if (this.columnsToJoin != null) {
            var columnNameOrigin = new SQLQuerySymbolOrigin.ColumnNameFromContext(combinedContext);
            for (SQLQuerySymbolEntry column : columnsToJoin) {
                if (column.isNotClassified()) {
                    SQLQuerySymbol symbol = column.getSymbol();
                    SQLQueryResultColumn leftColumnDef = left.resolveColumn(statistics.getMonitor(), column.getName());
                    SQLQueryResultColumn rightColumnDef = right.resolveColumn(statistics.getMonitor(), column.getName());
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
            this.conditionScope.setSymbolsOrigin(columnNameOrigin);
        } else {
            var conditionOrigin = new SQLQuerySymbolOrigin.ValueRefFromContext(combinedContext);
            this.setTailOrigin(conditionOrigin);

            if (this.condition != null) {
                this.condition.propagateContext(combinedContext, statistics);
                this.conditionScope.setSymbolsOrigin(conditionOrigin);
            }
        }

        return combinedContext;
    }

    @NotNull
    @Override
    protected SQLQueryRowsSourceContext resolveRowSourcesImpl(
        @NotNull SQLQueryRowsSourceContext context,
        @NotNull SQLQueryRecognitionContext statistics
    ) {
        context = this.left.resolveRowSources(context, statistics).combine(this.right.resolveRowSources(context, statistics));

        if (this.condition != null) {
            this.condition.resolveRowSources(context, statistics);
        }

        return context;
    }

    @NotNull
    @Override
    protected SQLQueryRowsDataContext resolveRowDataImpl(
        @NotNull SQLQueryRowsDataContext context,
        @NotNull SQLQueryRecognitionContext statistics
    ) {
        SQLQueryRowsDataContext x = this.left.getRowsDataContext().combine(this.right.getRowsDataContext());
        SQLQueryRowsDataContext combinedContext = this.getRowsSources().makeTuple(this, x.getColumnsList(), x.getPseudoColumnsList());

        if (this.columnsToJoin != null) {
            var columnNameOrigin = new SQLQuerySymbolOrigin.RowsDataRef(combinedContext);
            for (SQLQuerySymbolEntry column : columnsToJoin) {
                if (column.isNotClassified()) {
                    SQLQuerySymbol symbol = column.getSymbol();
                    SQLQueryResultColumn leftColumnDef = this.left.getRowsDataContext()
                        .resolveColumn(statistics.getMonitor(), column.getName());
                    SQLQueryResultColumn rightColumnDef = this.right.getRowsDataContext()
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
            this.conditionScope.setSymbolsOrigin(columnNameOrigin);
        } else {
            var conditionOrigin = new SQLQuerySymbolOrigin.RowsDataRef(combinedContext);
            this.setTailOrigin(conditionOrigin);

            if (this.condition != null) {
                this.condition.resolveValueRelations(combinedContext, statistics);
                this.conditionScope.setSymbolsOrigin(conditionOrigin);
            }
        }

        return combinedContext;
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
        return visitor.visitRowsNaturalJoin(this, arg);
    }
}