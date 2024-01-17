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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.SQLDialect.ProjectionAliasVisibilityScope;
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SQLQueryResultTupleContext.SQLQueryResultColumn;

import java.util.EnumSet;
import java.util.List;

public class SQLQueryRowsProjectionModel extends SQLQueryRowsSourceModel {
    private final SQLQueryRowsSourceModel fromSource; // from tableExpression
    private final SQLQuerySelectionResultModel result; // selectList

    private final SQLQueryValueExpression whereClause;
    private final SQLQueryValueExpression havingClause;
    private final SQLQueryValueExpression groupByClause;
    private final SQLQueryValueExpression orderByClause;


    public SQLQueryRowsProjectionModel(
        @NotNull Interval range,
        @NotNull SQLQueryRowsSourceModel fromSource,
        @NotNull SQLQuerySelectionResultModel result
    ) {
        this(range, fromSource, result, null, null, null, null);
    }

    public SQLQueryRowsProjectionModel(
        @NotNull Interval range,
        @NotNull SQLQueryRowsSourceModel fromSource,
        @NotNull SQLQuerySelectionResultModel result,
        @Nullable SQLQueryValueExpression whereClause,
        @Nullable SQLQueryValueExpression havingClause,
        @Nullable SQLQueryValueExpression groupByClause,
        @Nullable SQLQueryValueExpression orderByClause
    ) {
        super(range);
        this.result = result;
        this.fromSource = fromSource;
        this.whereClause = whereClause;
        this.havingClause = havingClause;
        this.groupByClause = groupByClause;
        this.orderByClause = orderByClause;
    }

    public SQLQueryRowsSourceModel getFromSource() {
        return fromSource;
    }

    public SQLQuerySelectionResultModel getResult() {
        return result;
    }

    public SQLQueryValueExpression getWhereClause() {
        return whereClause;
    }

    public SQLQueryValueExpression getHavingClause() {
        return havingClause;
    }

    public SQLQueryValueExpression getGroupByClause() {
        return groupByClause;
    }

    public SQLQueryValueExpression getOrderByClause() {
        return orderByClause;
    }

    @NotNull
    @Override
    protected SQLQueryDataContext propagateContextImpl(
        @NotNull SQLQueryDataContext context,
        @NotNull SQLQueryRecognitionContext statistics
    ) {
        SQLQueryDataContext unresolvedResult = fromSource.propagateContext(context, statistics);
        EnumSet<ProjectionAliasVisibilityScope> aliasScope = context.getDialect().getProjectionAliasVisibilityScope();

        List<SQLQueryResultColumn> resultColumns = this.result.expandColumns(unresolvedResult, this, statistics);
        SQLQueryDataContext resolvedResult = unresolvedResult.overrideResultTuple(resultColumns);

        if (this.whereClause != null) {
            this.whereClause.propagateContext(
                aliasScope.contains(ProjectionAliasVisibilityScope.WHERE) ? resolvedResult : unresolvedResult,
                statistics
            );
        }
        if (this.havingClause != null) {
            this.havingClause.propagateContext(
                aliasScope.contains(ProjectionAliasVisibilityScope.HAVING) ? resolvedResult : unresolvedResult,
                statistics
            );
        }
        if (this.groupByClause != null) {
            this.groupByClause.propagateContext(
                aliasScope.contains(ProjectionAliasVisibilityScope.GROUP_BY) ? resolvedResult : unresolvedResult,
                statistics
            );
        }
        if (this.orderByClause != null) {
            this.orderByClause.propagateContext(
                aliasScope.contains(ProjectionAliasVisibilityScope.ORDER_BY) ? resolvedResult : unresolvedResult,
                statistics
            );
        }
        return resolvedResult.hideSources();
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T node) {
        return visitor.visitRowsProjection(this, node);
    }
}