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
package org.jkiss.dbeaver.model.sql.semantics.model.select;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.SQLDialect.ProjectionAliasVisibilityScope;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryLexicalScope;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryResultColumn;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryResultPseudoColumn;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.sql.semantics.model.expressions.SQLQueryValueExpression;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Describes SELECT clause
 */
public class SQLQueryRowsProjectionModel extends SQLQueryRowsSourceModel {

    public static class FiltersData<T> {

        private static final FiltersData<?> EMPTY = new FiltersData<>(null, null, null, null);

        @SuppressWarnings("unchecked")
        public static <T> FiltersData<T> empty() {
            return (FiltersData<T>) EMPTY;
        }

        @NotNull
        public static <T> FiltersData<T> of(T where, T groupBy, T having, T orderBy) {
            return new FiltersData<T>(where, groupBy, having, orderBy);
        }

        public final T whereClause;
        public final T groupByClause;
        public final T havingClause;
        public final T orderByClause;

        private FiltersData(T where, T groupBy, T having, T orderBy) {
            this.whereClause = where;
            this.groupByClause = groupBy;
            this.havingClause = having;
            this.orderByClause = orderBy;
        }
    }

    @NotNull
    private final SQLQueryLexicalScope selectListScope;
    @NotNull
    
    private final SQLQueryRowsSourceModel fromSource; // from tableExpression
    @NotNull
    private final SQLQuerySelectionResultModel result; // selectList

    @NotNull
    private final FiltersData<SQLQueryValueExpression> filterExprs;
    @NotNull
    private final FiltersData<SQLQueryLexicalScope> filterScopes;

    public SQLQueryRowsProjectionModel(
        @NotNull STMTreeNode syntaxNode,
        @NotNull SQLQueryLexicalScope selectListScope,
        @NotNull SQLQueryRowsSourceModel fromSource,
        @NotNull SQLQuerySelectionResultModel result
    ) {
        this(syntaxNode, selectListScope, fromSource, null, FiltersData.empty(), FiltersData.empty(), result);
    }

    public SQLQueryRowsProjectionModel(
        @NotNull STMTreeNode syntaxNode,
        @NotNull SQLQueryLexicalScope selectListScope,
        @NotNull SQLQueryRowsSourceModel fromSource,
        @Nullable SQLQueryLexicalScope fromScope,
        @NotNull FiltersData<SQLQueryValueExpression> filterExprs,
        @NotNull FiltersData<SQLQueryLexicalScope> filterScopes,
        @NotNull SQLQuerySelectionResultModel result
    ) {
        super(syntaxNode, fromSource, result, filterExprs.whereClause, filterExprs.havingClause, filterExprs.groupByClause, filterExprs.orderByClause);
        this.result = result;
        this.selectListScope = selectListScope;
        this.fromSource = fromSource;
        this.filterExprs = filterExprs;
        this.filterScopes = filterScopes;

        this.registerLexicalScope(selectListScope);
        if (fromScope != null) {
            this.registerLexicalScope(fromScope);
        }
        Stream.of(filterScopes.whereClause, filterScopes.havingClause, filterScopes.groupByClause, filterScopes.orderByClause)
            .filter(Objects::nonNull).forEach(this::registerLexicalScope);
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
        return this.filterExprs.whereClause;
    }

    @Nullable
    public SQLQueryValueExpression getHavingClause() {
        return this.filterExprs.havingClause;
    }

    @Nullable
    public SQLQueryValueExpression getGroupByClause() {
        return this.filterExprs.groupByClause;
    }

    @Nullable
    public SQLQueryValueExpression getOrderByClause() {
        return this.filterExprs.orderByClause;
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
        List<SQLQueryResultPseudoColumn> resultPseudoColumns = unresolvedResult.getPseudoColumnsList().stream()
            .filter(s -> s.propagationPolicy.projected).toList();
        SQLQueryDataContext resolvedResult = unresolvedResult.overrideResultTuple(this, resultColumns, resultPseudoColumns);

        SQLQueryDataContext filtersContext = unresolvedResult.combine(resolvedResult);
        if (this.filterExprs.whereClause != null) {
            SQLQueryDataContext clauseCtx = aliasScope.contains(ProjectionAliasVisibilityScope.WHERE) ? filtersContext : unresolvedResult;
            this.filterExprs.whereClause.propagateContext(clauseCtx, statistics);
            this.filterScopes.whereClause.setContext(clauseCtx);
        }
        if (this.filterExprs.havingClause != null) {
            SQLQueryDataContext clauseCtx = aliasScope.contains(ProjectionAliasVisibilityScope.HAVING) ? filtersContext : unresolvedResult;
            this.filterExprs.havingClause.propagateContext(clauseCtx, statistics);
            this.filterScopes.havingClause.setContext(clauseCtx);
        }
        if (this.filterExprs.groupByClause != null) { // TODO consider dropping certain pseudocolumns
            SQLQueryDataContext clauseCtx = aliasScope.contains(ProjectionAliasVisibilityScope.GROUP_BY) ? filtersContext : unresolvedResult;
            this.filterExprs.groupByClause.propagateContext(clauseCtx, statistics);
            this.filterScopes.groupByClause.setContext(clauseCtx);
        }
        if (this.filterExprs.orderByClause != null) {
            SQLQueryDataContext clauseCtx = aliasScope.contains(ProjectionAliasVisibilityScope.ORDER_BY) ? filtersContext : unresolvedResult;
            this.filterExprs.orderByClause.propagateContext(clauseCtx, statistics);
            this.filterScopes.orderByClause.setContext(clauseCtx);
        }
        return resolvedResult.hideSources();
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
        return visitor.visitRowsProjection(this, arg);
    }
}