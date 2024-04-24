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
package org.jkiss.dbeaver.model.sql.semantics.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.SQLDialect.ProjectionAliasVisibilityScope;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryLexicalScope;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryResultColumn;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

import java.util.EnumSet;
import java.util.List;

/**
 * Describes SELECT clause
 */
public class SQLQueryRowsProjectionModel extends SQLQueryRowsSourceModel {
    @NotNull
    private final SQLQueryLexicalScope selectListScope;
    @NotNull
    
    private final SQLQueryRowsSourceModel fromSource; // from tableExpression
    @NotNull
    private final SQLQuerySelectionResultModel result; // selectList

    @Nullable
    private final SQLQueryValueExpression whereClause;
    @Nullable
    private final SQLQueryValueExpression havingClause;
    @Nullable
    private final SQLQueryValueExpression groupByClause;
    @Nullable
    private final SQLQueryValueExpression orderByClause;


    public SQLQueryRowsProjectionModel(
        @NotNull STMTreeNode syntaxNode,
        @NotNull SQLQueryLexicalScope selectListScope,
        @NotNull SQLQueryRowsSourceModel fromSource,
        @NotNull SQLQuerySelectionResultModel result
    ) {
        this(syntaxNode, selectListScope, fromSource, result, null, null, null, null);
    }

    public SQLQueryRowsProjectionModel(
        @NotNull STMTreeNode syntaxNode,
        @NotNull SQLQueryLexicalScope selectListScope,
        @NotNull SQLQueryRowsSourceModel fromSource,
        @NotNull SQLQuerySelectionResultModel result,
        @Nullable SQLQueryValueExpression whereClause,
        @Nullable SQLQueryValueExpression havingClause,
        @Nullable SQLQueryValueExpression groupByClause,
        @Nullable SQLQueryValueExpression orderByClause
    ) {
        super(syntaxNode, fromSource, result, whereClause, havingClause, groupByClause, orderByClause);
        this.result = result;
        this.selectListScope = selectListScope;
        this.fromSource = fromSource;
        this.whereClause = whereClause;
        this.havingClause = havingClause;
        this.groupByClause = groupByClause;
        this.orderByClause = orderByClause;
        
        this.registerLexicalScope(selectListScope);
    }

    @NotNull
    public SQLQueryRowsSourceModel getFromSource() {
        return fromSource;
    }

    @NotNull
    public SQLQuerySelectionResultModel getResult() {
        return result;
    }

    @Nullable
    public SQLQueryValueExpression getWhereClause() {
        return whereClause;
    }

    @Nullable
    public SQLQueryValueExpression getHavingClause() {
        return havingClause;
    }

    @Nullable
    public SQLQueryValueExpression getGroupByClause() {
        return groupByClause;
    }

    @Nullable
    public SQLQueryValueExpression getOrderByClause() {
        return orderByClause;
    }

    @NotNull
    @Override
    protected SQLQueryDataContext propagateContextImpl(
        @NotNull SQLQueryDataContext context,
        @NotNull SQLQueryRecognitionContext statistics
    ) {
        SQLQueryDataContext unresolvedResult = this.fromSource.propagateContext(context, statistics);
        this.selectListScope.setContext(unresolvedResult);
        EnumSet<ProjectionAliasVisibilityScope> aliasScope = context.getDialect().getProjectionAliasVisibilityScope();

        List<SQLQueryResultColumn> resultColumns = this.result.expandColumns(unresolvedResult, this, statistics);
        SQLQueryDataContext resolvedResult = unresolvedResult.overrideResultTuple(resultColumns);

        SQLQueryDataContext filtersContext = resolvedResult.combine(unresolvedResult);
        if (this.whereClause != null) {
            this.whereClause.propagateContext(
                aliasScope.contains(ProjectionAliasVisibilityScope.WHERE) ? filtersContext : unresolvedResult,
                statistics
            );
        }
        if (this.havingClause != null) {
            this.havingClause.propagateContext(
                aliasScope.contains(ProjectionAliasVisibilityScope.HAVING) ? filtersContext : unresolvedResult,
                statistics
            );
        }
        if (this.groupByClause != null) {
            this.groupByClause.propagateContext(
                aliasScope.contains(ProjectionAliasVisibilityScope.GROUP_BY) ? filtersContext : unresolvedResult,
                statistics
            );
        }
        if (this.orderByClause != null) {
            this.orderByClause.propagateContext(
                aliasScope.contains(ProjectionAliasVisibilityScope.ORDER_BY) ? filtersContext : unresolvedResult,
                statistics
            );
        }
        return resolvedResult.hideSources();
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
        return visitor.visitRowsProjection(this, arg);
    }
}