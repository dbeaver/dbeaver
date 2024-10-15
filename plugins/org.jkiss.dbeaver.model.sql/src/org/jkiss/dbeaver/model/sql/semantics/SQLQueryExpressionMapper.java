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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardParser;
import org.jkiss.dbeaver.model.sql.semantics.model.expressions.SQLQueryValueExpression;
import org.jkiss.dbeaver.model.sql.semantics.model.expressions.SQLQueryValueTupleReferenceExpression;
import org.jkiss.dbeaver.model.sql.semantics.model.select.*;
import org.jkiss.dbeaver.model.stm.STMKnownRuleNames;
import org.jkiss.dbeaver.model.stm.STMTreeNode;
import org.jkiss.dbeaver.model.stm.STMTreeTermNode;

import java.util.*;

class SQLQueryExpressionMapper extends SQLQueryTreeMapper<SQLQueryRowsSourceModel, SQLQueryModelRecognizer> {

    private static final Log log = Log.getLog(SQLQueryExpressionMapper.class);

    public SQLQueryExpressionMapper(@NotNull SQLQueryModelRecognizer recognizer) {
        super(SQLQueryRowsSourceModel.class, queryExpressionSubtreeNodeNames, translations, recognizer);
    }

    @NotNull
    private static final Set<String> queryExpressionSubtreeNodeNames = Set.of(
        STMKnownRuleNames.sqlQuery,
        STMKnownRuleNames.directSqlDataStatement,
        STMKnownRuleNames.selectStatement,
        STMKnownRuleNames.withClause,
        STMKnownRuleNames.cteList,
        STMKnownRuleNames.with_list_element,
        STMKnownRuleNames.subquery,
        STMKnownRuleNames.unionTerm,
        STMKnownRuleNames.exceptTerm,
        STMKnownRuleNames.nonJoinQueryExpression,
        STMKnownRuleNames.nonJoinQueryTerm,
        STMKnownRuleNames.intersectTerm,
        STMKnownRuleNames.nonJoinQueryPrimary,
        STMKnownRuleNames.simpleTable,
        STMKnownRuleNames.querySpecification,
        STMKnownRuleNames.tableExpression,
        STMKnownRuleNames.queryPrimary,
        STMKnownRuleNames.queryTerm,
        STMKnownRuleNames.queryExpression,
        STMKnownRuleNames.selectStatementSingleRow,
        STMKnownRuleNames.fromClause,
        STMKnownRuleNames.nonjoinedTableReference,
        STMKnownRuleNames.tableReference,
        STMKnownRuleNames.joinedTable,
        STMKnownRuleNames.derivedTable,
        STMKnownRuleNames.tableSubquery,
        STMKnownRuleNames.crossJoinTerm,
        STMKnownRuleNames.naturalJoinTerm,
        STMKnownRuleNames.explicitTable
    );

    @NotNull
    private static final Map<String, TreeMapperCallback<SQLQueryRowsSourceModel, SQLQueryModelRecognizer>> translations = Map.of(
        STMKnownRuleNames.directSqlDataStatement, (n, cc, r) -> {
            if (cc.isEmpty()) {
                return null;
            } else if (cc.size() == 1) {
                return cc.get(0);
            } else {
                List<SQLQueryRowsSourceModel> subqueries = cc.subList(0, cc.size() - 1);
                SQLQueryRowsSourceModel resultQuery = cc.get(cc.size() - 1);
                if (findImmediateChild(n, resultQuery.getSyntaxNode()) != n.findLastNonErrorChild()) {
                    resultQuery = makeEmptyRowsModel(n.findLastNonErrorChild());
                }

                STMTreeNode withNode = n.findFirstChildOfName(STMKnownRuleNames.withClause);
                if (withNode != null) {
                    boolean isRecursive = withNode.findFirstChildOfName(STMKnownRuleNames.RECURSIVE_TERM) != null;
                    List<SQLQueryRowsCteSubqueryModel> cteSubqueries = new ArrayList<>();

                    STMTreeNode cteListNode = withNode.findLastChildOfName(STMKnownRuleNames.cteList);
                    if (cteListNode != null) {

                        SubsourcesMap subsources = new SubsourcesMap(subqueries, cteListNode);

                        for (STMTreeNode cteSubqueryNode : cteListNode.findChildrenOfName(STMKnownRuleNames.with_list_element)) {
                            STMTreeNode subqueryNameNode = cteSubqueryNode.findFirstChildOfName(STMKnownRuleNames.queryName);
                            SQLQuerySymbolEntry subqueryName = subqueryNameNode == null ? null : r.collectIdentifier(subqueryNameNode);

                            STMTreeNode columnListNode = cteSubqueryNode.findFirstChildOfName(STMKnownRuleNames.columnNameList);
                            List<SQLQuerySymbolEntry> columnList = columnListNode == null
                                ? Collections.emptyList()
                                : r.collectColumnNameList(columnListNode);

                            SQLQueryRowsSourceModel subquerySource = subsources.getOrEmpty(cteSubqueryNode);
                            cteSubqueries.add(new SQLQueryRowsCteSubqueryModel(cteSubqueryNode, subqueryName, columnList, subquerySource));
                        }
                    }
                    return new SQLQueryRowsCteModel(n, isRecursive, cteSubqueries, resultQuery);
                } else {
                    return new SQLQueryRowsCteModel(n, false, Collections.emptyList(), resultQuery);
                }
            }
        },
        STMKnownRuleNames.queryExpression, (n, cc, r) -> {
            if (cc.isEmpty()) {
                return makeEmptyRowsModel(n);
            } else {
                SubsourcesMap subsources = new SubsourcesMap(cc, n);
                List<STMTreeNode> childNodes = n.findNonErrorChildren();
                SQLQueryRowsSourceModel source = subsources.getOrEmpty(childNodes.get(0));
                for (STMTreeNode childNode : childNodes.subList(1, childNodes.size())) {
                    List<SQLQuerySymbolEntry> corresponding = r.collectColumnNameList(childNode);
                    SQLQueryRowsSourceModel nextSource = subsources.getOrEmpty(childNode);
                    Interval range = Interval.of(n.getRealInterval().a, childNode.getRealInterval().b);
                    SQLQueryRowsSetCorrespondingOperationKind opKind = switch (childNode.getNodeKindId()) {
                        case SQLStandardParser.RULE_exceptTerm -> SQLQueryRowsSetCorrespondingOperationKind.EXCEPT;
                        case SQLStandardParser.RULE_unionTerm -> SQLQueryRowsSetCorrespondingOperationKind.UNION;
                        default ->
                            throw new UnsupportedOperationException("Unexpected child node kind at queryExpression: " + childNode.getNodeName());
                    };
                    source = new SQLQueryRowsSetCorrespondingOperationModel(range, childNode, source, nextSource, corresponding, opKind);
                }
                return source;
            }
        },
        STMKnownRuleNames.nonJoinQueryTerm, (n, cc, r) -> {
            if (cc.isEmpty()) {
                return makeEmptyRowsModel(n);
            } else {
                SubsourcesMap subsources = new SubsourcesMap(cc, n);
                List<STMTreeNode> childNodes = n.findNonErrorChildren();
                SQLQueryRowsSourceModel source = subsources.getOrEmpty(childNodes.get(0));
                for (STMTreeNode childNode : childNodes.subList(1, childNodes.size())) {
                    List<SQLQuerySymbolEntry> corresponding = r.collectColumnNameList(childNode);
                    SQLQueryRowsSourceModel nextSource = subsources.getOrEmpty(childNode);
                    Interval range = Interval.of(n.getRealInterval().a, childNode.getRealInterval().b);
                    SQLQueryRowsSetCorrespondingOperationKind opKind = switch (childNode.getNodeKindId()) {
                        case SQLStandardParser.RULE_intersectTerm ->
                            SQLQueryRowsSetCorrespondingOperationKind.INTERSECT;
                        default ->
                            throw new UnsupportedOperationException("Unexpected child node kind at nonJoinQueryTerm: " + childNode.getNodeName());
                    };
                    source = new SQLQueryRowsSetCorrespondingOperationModel(range, childNode, source, nextSource, corresponding, opKind);
                }
                return source;
            }
        },
        STMKnownRuleNames.joinedTable, (n, cc, r) -> {
            // joinedTable: (nonjoinedTableReference|(LeftParen joinedTable RightParen)) (naturalJoinTerm|crossJoinTerm)+;
            if (cc.isEmpty()) {
                return makeEmptyRowsModel(n);
            } else {
                SubsourcesMap subsources = new SubsourcesMap(cc, n);
                List<STMTreeNode> childNodes = n.findNonErrorChildren();
                SQLQueryRowsSourceModel source = subsources.getOrEmpty(childNodes.get(0));
                for (STMTreeNode childNode : childNodes.subList(1, childNodes.size())) {
                    if (!(childNode instanceof STMTreeTermNode)) {
                        final SQLQueryRowsSourceModel currSource = source;
                        final SQLQueryRowsSourceModel nextSource = subsources.getOrEmpty(childNode);
                        // TODO see second case of the first source if parens are correctly ignored here
                        Interval range = Interval.of(n.getRealInterval().a, childNode.getRealInterval().b);
                        source = switch (childNode.getNodeKindId()) {
                            case SQLStandardParser.RULE_naturalJoinTerm -> {
                                Optional<STMTreeNode> joinConditionNode =
                                    Optional.ofNullable(childNode.findFirstChildOfName(STMKnownRuleNames.joinSpecification))
                                        .map(cn -> cn.findFirstChildOfName(STMKnownRuleNames.joinCondition));
                                if (joinConditionNode.isPresent()) {
                                    try (SQLQueryModelRecognizer.LexicalScopeHolder condScope = r.openScope()) {
                                        condScope.lexicalScope.registerSyntaxNode(joinConditionNode.get());
                                        yield joinConditionNode.map(cn -> cn.findFirstChildOfName(STMKnownRuleNames.searchCondition))
                                            .map(r::collectValueExpression)
                                            .map(e -> new SQLQueryRowsNaturalJoinModel(range, childNode, currSource, nextSource, e,
                                                condScope.lexicalScope))
                                            .orElseGet(() -> new SQLQueryRowsNaturalJoinModel(range, childNode, currSource, nextSource,
                                                Collections.emptyList()));
                                    }
                                } else {
                                    yield new SQLQueryRowsNaturalJoinModel(range, childNode, currSource, nextSource,
                                        r.collectColumnNameList(childNode));
                                }
                            }
                            case SQLStandardParser.RULE_crossJoinTerm ->
                                new SQLQueryRowsCrossJoinModel(range, childNode, currSource, nextSource);
                            default -> throw new UnsupportedOperationException(
                                "Unexpected child node kind at queryExpression: " + childNode.getNodeName());
                        };
                    }
                }
                return source;
            }
        },
        STMKnownRuleNames.fromClause, (n, cc, r) -> {
            if (cc.isEmpty()) {
                return makeEmptyRowsModel(n);
            } else {
                SubsourcesMap subsources = new SubsourcesMap(cc, n);
                List<STMTreeNode> childNodes = n.findChildrenOfName(STMKnownRuleNames.tableReference);
                SQLQueryRowsSourceModel source = subsources.getOrEmpty(childNodes.get(0));
                for (STMTreeNode childNode : childNodes.subList(1, childNodes.size())) {
                    SQLQueryRowsSourceModel nextSource = subsources.getOrEmpty(childNode);
                    if (nextSource != null) {
                        Interval range = Interval.of(n.getRealInterval().a, childNode.getRealInterval().b);
                        source = switch (childNode.getNodeKindId()) {
                            case SQLStandardParser.RULE_tableReference ->
                                new SQLQueryRowsCrossJoinModel(range, childNode, source, nextSource);
                            default -> throw new UnsupportedOperationException(
                                "Unexpected child node kind at fromClause: " + childNode.getNodeName());
                        };
                    } else {
                        // certain tableReference subtree was not recognized correctly, consider error message
                    }
                }
                return source;
            }
        },
        STMKnownRuleNames.querySpecification, (n, cc, r) -> {
            STMTreeNode selectListNode = n.findFirstChildOfName(STMKnownRuleNames.selectList);
            if (selectListNode == null) {
                log.debug("Invalid querySpecification: missing selectList");
                return makeEmptyRowsModel(n);
            }

            List<STMTreeNode> selectSublists = selectListNode.findChildrenOfName(STMKnownRuleNames.selectSublist);
            SQLQuerySelectionResultModel resultModel = new SQLQuerySelectionResultModel(selectListNode, selectSublists.size());

            SQLQueryLexicalScope selectListScope;
            STMTreeNode selectKeywordNode;
            try (SQLQueryModelRecognizer.LexicalScopeHolder selectListScopeHolder = r.openScope()) {
                selectListScope = selectListScopeHolder.lexicalScope;
                selectKeywordNode = n.findFirstChildOfName(STMKnownRuleNames.SELECT_TERM);
                if (selectKeywordNode == null) {
                    log.debug("SELECT keyword is missing");
                    return makeEmptyRowsModel(n);
                }

                for (STMTreeNode selectSublist : selectSublists) {
                    STMTreeNode sublistNode = selectSublist.findFirstNonErrorChild();
                    if (sublistNode != null) {
                        switch (sublistNode.getNodeKindId()) { // selectSublist: (Asterisk|derivedColumn|qualifier Period Asterisk
                            case SQLStandardParser.RULE_derivedColumn -> {
                                // derivedColumn: valueExpression (asClause)?; asClause: (AS)? columnName;
                                STMTreeNode exprNode = sublistNode.findFirstChildOfName(STMKnownRuleNames.valueExpression);
                                SQLQueryValueExpression expr = exprNode == null ? null : r.collectValueExpression(exprNode);
                                if (expr instanceof SQLQueryValueTupleReferenceExpression tupleRef) {
                                    resultModel.addTupleSpec(sublistNode, tupleRef);
                                } else {
                                    STMTreeNode asClauseNode = sublistNode.findLastChildOfName(STMKnownRuleNames.asClause);
                                    if (asClauseNode != null) {
                                        STMTreeNode columnNameNode = asClauseNode.findLastChildOfName(STMKnownRuleNames.columnName);
                                        SQLQuerySymbolEntry asColumnName = columnNameNode == null ? null : r.collectIdentifier(columnNameNode);
                                        resultModel.addColumnSpec(sublistNode, expr, asColumnName);
                                    } else {
                                        resultModel.addColumnSpec(sublistNode, expr);
                                    }
                                }
                            }
                            case SQLStandardParser.RULE_anyUnexpected -> {
                                // TODO register these pieces in the lexical scope
                                // error in query text, ignoring it
                            }
                            default -> {
                                resultModel.addCompleteTupleSpec(sublistNode);
                            }
                        }
                    }
                }
            }

            SQLQueryRowsSourceModel source = cc.isEmpty() ? makeEmptyRowsModel(n) : cc.get(0);
            STMTreeNode tableExpr = n.findFirstChildOfName(STMKnownRuleNames.tableExpression);
            SQLQueryRowsProjectionModel projectionModel;
            if (tableExpr != null) {
                selectListScope.setInterval(Interval.of(selectKeywordNode.getRealInterval().a, tableExpr.getRealInterval().a));

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
                            try (SQLQueryModelRecognizer.LexicalScopeHolder exprScope = r.openScope()) {
                                filterExprs[i] = r.collectValueExpression(filterNode);
                                nextScopeNodes[prevScopeIndex] = filterNode;
                                scopes[scopeIndex] = exprScope.lexicalScope;
                                prevScopes[scopeIndex] = scopes[prevScopeIndex];
                                prevScopeIndex = scopeIndex;
                            }
                        }
                    }
                }
                for (int i = 0; i < scopes.length; i++) {
                    SQLQueryLexicalScope scope = scopes[i];
                    if (scope != null) {
                        int from = prevScopes[i].getInterval().b;
                        int to = nextScopeNodes[i] != null ? nextScopeNodes[i].getRealInterval().a : Integer.MAX_VALUE;
                        scope.setInterval(Interval.of(from, to));
                    }
                }

                projectionModel = new SQLQueryRowsProjectionModel(
                    n, selectListScope, source, fromScope,
                    SQLQueryRowsProjectionModel.FiltersData.of(filterExprs[0], filterExprs[1], filterExprs[2], filterExprs[3]),
                    SQLQueryRowsProjectionModel.FiltersData.of(scopes[1], scopes[2], scopes[3], scopes[4]),
                    resultModel
                );
            } else {
                projectionModel = new SQLQueryRowsProjectionModel(n, selectListScope, source, resultModel);
            }
            return projectionModel;
        },
        STMKnownRuleNames.nonjoinedTableReference, (n, cc, r) -> {
            // can they both be missing?
            SQLQueryRowsSourceModel source = cc.isEmpty() ? r.collectTableReference(n, false) : cc.get(0);

            // TODO column reference at PARTITION clause

            STMTreeNode correlationSpecNode = n.findLastChildOfName(STMKnownRuleNames.correlationSpecification);
            if (correlationSpecNode != null) {
                STMTreeNode correlationNameNode = correlationSpecNode.findFirstChildOfName(STMKnownRuleNames.correlationName);
                SQLQuerySymbolEntry correlationName = correlationNameNode == null ? null : r.collectIdentifier(correlationNameNode);
                if (correlationName != null) {
                    source = new SQLQueryRowsCorrelatedSourceModel(n, source, correlationName, r.collectColumnNameList(correlationSpecNode));
                }
            }
            return source;
        },
        STMKnownRuleNames.explicitTable, (n, cc, r) -> r.collectTableReference(n, false),
        STMKnownRuleNames.tableValueConstructor, (n, cc, r) -> {
            List<SQLQueryValueExpression> values = n.findChildrenOfName(STMKnownRuleNames.rowValueConstructor).stream()
                .map(r::collectValueExpression).toList();
            boolean isIncomplete = n.getChildCount() != values.size() * 2 || n.hasErrorChildren();
            return new SQLQueryRowsTableValueModel(n, values, isIncomplete);
        }
    );

    private static class SubsourcesMap {
        private final Map<STMTreeNode, SQLQueryRowsSourceModel> subsourceByNode;

        public SubsourcesMap(@NotNull List<SQLQueryRowsSourceModel> subqueries, @NotNull STMTreeNode subroot) {
            this.subsourceByNode = new HashMap<>(subqueries.size());
            for (SQLQueryRowsSourceModel subquery : subqueries) {
                STMTreeNode subrootChild = findImmediateChild(subroot, subquery.getSyntaxNode());
                this.subsourceByNode.put(subrootChild, subquery);
            }
        }

        @NotNull
        public SQLQueryRowsSourceModel getOrEmpty(STMTreeNode subrootsChild) {
            SQLQueryRowsSourceModel source = this.subsourceByNode.get(subrootsChild);
            if (source == null) {
                source = makeEmptyRowsModel(subrootsChild);
            }
            return source;
        }
    }

    private static STMTreeNode findImmediateChild(STMTreeNode subroot, STMTreeNode deeperChild) {
        STMTreeNode current = deeperChild;
        STMTreeNode parent = current.getParentNode();
        while (parent != subroot && parent != null) {
            current = parent;
            parent = current.getParentNode();
        }
        return current;
    }

    /**
     * Prepare fake table rows source used to represent invalid query fragments when the adequate semantic model item cannot be constructed
     */
    public static SQLQueryRowsSourceModel makeEmptyRowsModel(STMTreeNode n) {
        return new SQLQueryRowsTableValueModel(n, Collections.emptyList(), true);
    }
}
