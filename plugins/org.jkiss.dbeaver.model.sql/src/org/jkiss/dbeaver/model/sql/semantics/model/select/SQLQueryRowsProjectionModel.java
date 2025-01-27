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

import org.antlr.v4.runtime.misc.Interval;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardParser;
import org.jkiss.dbeaver.model.sql.SQLDialect.ProjectionAliasVisibilityScope;
import org.jkiss.dbeaver.model.sql.semantics.*;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryResultColumn;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryResultPseudoColumn;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.sql.semantics.model.expressions.SQLQueryValueExpression;
import org.jkiss.dbeaver.model.sql.semantics.model.expressions.SQLQueryValueTupleReferenceExpression;
import org.jkiss.dbeaver.model.stm.STMKnownRuleNames;
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

    private static final Log log = Log.getLog(SQLQueryRowsProjectionModel.class);

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

    @Nullable
    private final SQLQueryLexicalScope fromScope;
    @Nullable
    private final SQLQueryLexicalScope tailScope;

    public SQLQueryRowsProjectionModel(
        @NotNull STMTreeNode syntaxNode,
        @NotNull SQLQueryLexicalScope selectListScope,
        @NotNull SQLQueryRowsSourceModel fromSource,
        @Nullable SQLQueryLexicalScope fromScope,
        @NotNull FiltersData<SQLQueryValueExpression> filterExprs,
        @NotNull FiltersData<SQLQueryLexicalScope> filterScopes,
        @NotNull SQLQuerySelectionResultModel result,
        @Nullable SQLQueryLexicalScope tailScope
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

        this.fromScope = fromScope;
        this.tailScope = tailScope;
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
        if (this.fromScope != null) {
            this.fromScope.setSymbolsOrigin(new SQLQuerySymbolOrigin.RowsetRefFromContext(context));
        }

        SQLQueryDataContext unresolvedResult = this.fromSource.propagateContext(context, statistics);
        this.selectListScope.setSymbolsOrigin(new SQLQuerySymbolOrigin.ValueRefFromContext(unresolvedResult));
        EnumSet<ProjectionAliasVisibilityScope> aliasVisibilities = context.getDialect().getProjectionAliasVisibilityScope();

        List<SQLQueryResultColumn> resultColumns = this.result.expandColumns(unresolvedResult, this, statistics);
        List<SQLQueryResultPseudoColumn> resultPseudoColumns = unresolvedResult.getPseudoColumnsList().stream()
            .filter(s -> s.propagationPolicy.projected).toList();
        SQLQueryDataContext resolvedResult = unresolvedResult.overrideResultTuple(this, resultColumns, resultPseudoColumns);

        SQLQueryDataContext filtersContext = unresolvedResult.combine(resolvedResult);
        if (this.filterExprs.whereClause != null) {
            SQLQueryDataContext clauseCtx = aliasVisibilities.contains(ProjectionAliasVisibilityScope.WHERE) ? filtersContext : unresolvedResult;
            this.filterExprs.whereClause.propagateContext(clauseCtx, statistics);
            this.filterScopes.whereClause.setSymbolsOrigin(new SQLQuerySymbolOrigin.ValueRefFromContext(clauseCtx));
        }
        if (this.filterExprs.havingClause != null) {
            SQLQueryDataContext clauseCtx = aliasVisibilities.contains(ProjectionAliasVisibilityScope.HAVING) ? filtersContext : unresolvedResult;
            this.filterExprs.havingClause.propagateContext(clauseCtx, statistics);
            this.filterScopes.havingClause.setSymbolsOrigin(new SQLQuerySymbolOrigin.ValueRefFromContext(clauseCtx));
        }
        if (this.filterExprs.groupByClause != null) { // TODO consider dropping certain pseudocolumns
            SQLQueryDataContext clauseCtx = aliasVisibilities.contains(ProjectionAliasVisibilityScope.GROUP_BY) ? filtersContext : unresolvedResult;
            this.filterExprs.groupByClause.propagateContext(clauseCtx, statistics);
            this.filterScopes.groupByClause.setSymbolsOrigin(new SQLQuerySymbolOrigin.ValueRefFromContext(clauseCtx));
        }
        if (this.filterExprs.orderByClause != null) {
            SQLQueryDataContext clauseCtx = aliasVisibilities.contains(ProjectionAliasVisibilityScope.ORDER_BY) ? filtersContext : unresolvedResult;
            this.filterExprs.orderByClause.propagateContext(clauseCtx, statistics);
            this.filterScopes.orderByClause.setSymbolsOrigin(new SQLQuerySymbolOrigin.ValueRefFromContext(clauseCtx));
        }

        if (this.tailScope != null) {
            this.setTailOrigin(this.tailScope.getSymbolsOrigin());
        }

        return resolvedResult.hideSources();
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
        return visitor.visitRowsProjection(this, arg);
    }

    public static SQLQueryRowsSourceModel recognize(
        @NotNull STMTreeNode n,
        @NotNull List<SQLQueryRowsSourceModel> sourceModels,
        @NotNull SQLQueryModelRecognizer recognizer
    ) {
        return recognize(n, sourceModels, recognizer, SQLQueryRowsProjectionModel::new);
    }

    @FunctionalInterface
    public interface ProjectionModelCtor {
        SQLQueryRowsSourceModel apply(
            @NotNull STMTreeNode syntaxNode,
            @NotNull SQLQueryLexicalScope selectListScope,
            @NotNull SQLQueryRowsSourceModel fromSource,
            @Nullable SQLQueryLexicalScope fromScope,
            @NotNull FiltersData<SQLQueryValueExpression> filterExprs,
            @NotNull FiltersData<SQLQueryLexicalScope> filterScopes,
            @NotNull SQLQuerySelectionResultModel result,
            @Nullable SQLQueryLexicalScope tailScope
        );
    }

    @NotNull
    public static SQLQueryRowsSourceModel recognize(
        @NotNull STMTreeNode syntaxNode,
        @NotNull List<SQLQueryRowsSourceModel> sourceModels,
        @NotNull  SQLQueryModelRecognizer recognizer,
        @NotNull ProjectionModelCtor ctor
    ) {
        STMTreeNode selectListNode = syntaxNode.findFirstChildOfName(STMKnownRuleNames.selectList);
        if (selectListNode == null) {
            log.debug("Invalid querySpecification: missing selectList");
            return SQLQueryExpressionMapper.makeEmptyRowsModel(syntaxNode);
        }

        List<STMTreeNode> selectSublists = selectListNode.findChildrenOfName(STMKnownRuleNames.selectSublist);
        SQLQuerySelectionResultModel resultModel = new SQLQuerySelectionResultModel(selectListNode, selectSublists.size());

        SQLQueryLexicalScope selectListScope;
        STMTreeNode selectKeywordNode;
        try (SQLQueryModelRecognizer.LexicalScopeHolder selectListScopeHolder = recognizer.openScope()) {
            selectListScope = selectListScopeHolder.lexicalScope;
            selectKeywordNode = syntaxNode.findFirstChildOfName(STMKnownRuleNames.SELECT_TERM);
            if (selectKeywordNode == null) {
                log.debug("SELECT keyword is missing in projection model");
                return SQLQueryExpressionMapper.makeEmptyRowsModel(syntaxNode);
            }

            for (STMTreeNode selectSublist : selectSublists) {
                STMTreeNode sublistNode = selectSublist.findFirstNonErrorChild();
                if (sublistNode != null) {
                    switch (sublistNode.getNodeKindId()) { // selectSublist: (Asterisk|derivedColumn|qualifier Period Asterisk
                        case SQLStandardParser.RULE_derivedColumn -> {
                            // derivedColumn: valueExpression (asClause)?; asClause: (AS)? columnName;
                            STMTreeNode exprNode = sublistNode.findFirstChildOfName(STMKnownRuleNames.valueExpression);
                            SQLQueryValueExpression expr = exprNode == null ? null : recognizer.collectValueExpression(exprNode);
                            if (expr instanceof SQLQueryValueTupleReferenceExpression tupleRef) {
                                resultModel.addTupleSpec(sublistNode, tupleRef);
                            } else {
                                STMTreeNode asClauseNode = sublistNode.findLastChildOfName(STMKnownRuleNames.asClause);
                                if (asClauseNode != null) {
                                    STMTreeNode columnNameNode = asClauseNode.findLastChildOfName(STMKnownRuleNames.columnName);
                                    SQLQuerySymbolEntry asColumnName = columnNameNode == null
                                        ? null
                                        : recognizer.collectIdentifier(columnNameNode, null);
                                    resultModel.addColumnSpec(sublistNode, expr, asColumnName);
                                } else {
                                    resultModel.addColumnSpec(sublistNode, expr);
                                }
                            }
                        }
                        case SQLStandardParser.RULE_anyUnexpected -> {
                            // TODO register these pieces in the lexical scope?
                            // error in query text, ignoring it
                        }
                        default -> {
                            resultModel.addCompleteTupleSpec(sublistNode);
                        }
                    }
                }
            }
        }

        STMTreeNode setQuantifierNode = syntaxNode.findFirstChildOfName(STMKnownRuleNames.setQuantifier);
        // keyword itself should not be included to the scope
        // antlr give us interval end as the pos of the las symbol of the keyword
        // so, move completion context to the place after the keyword and space
        int selectListScopeStart = (setQuantifierNode != null ? setQuantifierNode : selectKeywordNode).getRealInterval().b + 2;

        SQLQueryRowsSourceModel source = sourceModels.isEmpty()
            ? SQLQueryExpressionMapper.makeEmptyRowsModel(syntaxNode)
            : sourceModels.get(0);
        STMTreeNode tableExpr = syntaxNode.findFirstChildOfName(STMKnownRuleNames.tableExpression);
        SQLQueryRowsSourceModel projectionModel;
        if (tableExpr != null) {
            selectListScope.setInterval(Interval.of(selectListScopeStart, tableExpr.getRealInterval().a));
            SQLQueryLexicalScope fromScope = new SQLQueryLexicalScope();
            STMTreeNode[] filterNodes = new STMTreeNode[]{
                tableExpr.findFirstChildOfName(STMKnownRuleNames.whereClause),
                tableExpr.findFirstChildOfName(STMKnownRuleNames.groupByClause),
                tableExpr.findFirstChildOfName(STMKnownRuleNames.havingClause),
                tableExpr.findFirstChildOfName(STMKnownRuleNames.orderByClause)
            };
            SQLQueryValueExpression[] filterExprs = new SQLQueryValueExpression[filterNodes.length];
            SQLQueryLexicalScope[] scopes = new SQLQueryLexicalScope[filterNodes.length + 1];
            SQLQueryLexicalScope[] prevScopes = new SQLQueryLexicalScope[filterNodes.length + 1];
            STMTreeNode[] nextScopeNodes = new STMTreeNode[filterNodes.length + 1];
            {
                scopes[0] = fromScope;
                prevScopes[0] = selectListScope;
                int prevScopeIndex = 0;
                for (int i = 0; i < filterNodes.length; i++) {
                    STMTreeNode filterNode = filterNodes[i];
                    int scopeIndex = i + 1;
                    if (filterNode != null) {
                        try (SQLQueryModelRecognizer.LexicalScopeHolder exprScope = recognizer.openScope()) {
                            filterExprs[i] = recognizer.collectValueExpression(filterNode);
                            nextScopeNodes[prevScopeIndex] = filterNode;
                            scopes[scopeIndex] = exprScope.lexicalScope;
                            prevScopes[scopeIndex] = scopes[prevScopeIndex];
                            prevScopeIndex = scopeIndex;
                        }
                    }
                }
            }
            SQLQueryLexicalScope tailScope = null;
            for (int i = 0; i < scopes.length; i++) {
                SQLQueryLexicalScope scope = scopes[i];
                if (scope != null) {
                    tailScope = scope;
                    int from = prevScopes[i].getInterval().b;
                    int to = nextScopeNodes[i] != null ? nextScopeNodes[i].getRealInterval().a : Integer.MAX_VALUE;
                    scope.setInterval(Interval.of(from, to));
                }
            }

            STMTreeNode limitNode = tableExpr.findFirstChildOfName(STMKnownRuleNames.limitClause);
            if (limitNode != null) {
                tailScope = null;
            }

            projectionModel = ctor.apply(
                syntaxNode, selectListScope, source, fromScope,
                SQLQueryRowsProjectionModel.FiltersData.of(filterExprs[0], filterExprs[1], filterExprs[2], filterExprs[3]),
                SQLQueryRowsProjectionModel.FiltersData.of(scopes[1], scopes[2], scopes[3], scopes[4]),
                resultModel, tailScope
            );
        } else {
            selectListScope.setInterval(Interval.of(selectListScopeStart, syntaxNode.getRealInterval().b));
            projectionModel = ctor.apply(
                syntaxNode, selectListScope, source, null, FiltersData.empty(), FiltersData.empty(), resultModel, selectListScope
            );
        }

        return projectionModel;
    }
}