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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardParser;
import org.jkiss.dbeaver.model.sql.semantics.model.select.*;
import org.jkiss.dbeaver.model.stm.STMKnownRuleNames;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

import java.util.*;

class SQLQueryExpressionMapper extends SQLQueryTreeMapper<SQLQueryRowsSourceModel, SQLQueryModelContext> {
    public SQLQueryExpressionMapper(@Nullable SQLQueryModelContext recognizer) {
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
    private static final Map<String, TreeMapperCallback<SQLQueryRowsSourceModel, SQLQueryModelContext>> translations = Map.of(
        STMKnownRuleNames.directSqlDataStatement, (n, cc, r) -> {
            if (cc.isEmpty()) {
                return null;
            } else if (cc.size() == 1) {
                return cc.get(0);
            } else {
                List<SQLQueryRowsSourceModel> subqueries = cc.subList(0, cc.size() - 1);
                SQLQueryRowsSourceModel resultQuery = cc.get(cc.size() - 1);

                STMTreeNode withNode = n.findChildOfName(STMKnownRuleNames.withClause);
                boolean isRecursive = withNode.getChildCount() > 2; // is RECURSIVE keyword presented

                SQLQueryRowsCteModel cte = new SQLQueryRowsCteModel(r, n, isRecursive, resultQuery);

                STMTreeNode cteListNode = withNode.getStmChild(withNode.getChildCount() - 1);
                for (int i = 0, j = 0; i < cteListNode.getChildCount() && j < subqueries.size(); i += 2, j++) {
                    STMTreeNode cteSubqueryNode = cteListNode.getStmChild(i);

                    SQLQuerySymbolEntry subqueryName = r.collectIdentifier(cteSubqueryNode.getStmChild(0));

                    STMTreeNode columnListNode = cteSubqueryNode.findChildOfName(STMKnownRuleNames.columnNameList);
                    List<SQLQuerySymbolEntry> columnList = columnListNode != null ? r.collectColumnNameList(columnListNode) : List.of();

                    SQLQueryRowsSourceModel subquerySource = subqueries.get(j);
                    cte.addSubquery(cteSubqueryNode, subqueryName, columnList, subquerySource);
                }

                return cte;
            }
        },
        STMKnownRuleNames.queryExpression, (n, cc, r) -> {
            if (cc.isEmpty()) {
                return r.getQueryDataContext().getDefaultTable(n);
            } else {
                SQLQueryRowsSourceModel source = cc.get(0);
                for (int i = 1; i < cc.size(); i++) {
                    STMTreeNode childNode = n.getStmChild(i);
                    List<SQLQuerySymbolEntry> corresponding = r.collectColumnNameList(childNode);
                    SQLQueryRowsSourceModel nextSource = cc.get(i);
                    Interval range = Interval.of(n.getRealInterval().a, childNode.getRealInterval().b);
                    SQLQueryRowsSetCorrespondingOperationKind opKind = switch (childNode.getNodeKindId()) {
                        case SQLStandardParser.RULE_exceptTerm -> SQLQueryRowsSetCorrespondingOperationKind.EXCEPT;
                        case SQLStandardParser.RULE_unionTerm -> SQLQueryRowsSetCorrespondingOperationKind.UNION;
                        default ->
                            throw new UnsupportedOperationException("Unexpected child node kind at queryExpression");
                    };
                    source = new SQLQueryRowsSetCorrespondingOperationModel(r, range, childNode, source, nextSource, corresponding, opKind);
                }
                return source;
            }
        },
        STMKnownRuleNames.nonJoinQueryTerm, (n, cc, r) -> {
            if (cc.isEmpty()) {
                return r.getQueryDataContext().getDefaultTable(n);
            } else {
                SQLQueryRowsSourceModel source = cc.get(0);
                for (int i = 1; i < cc.size(); i++) {
                    STMTreeNode childNode = n.getStmChild(i);
                    List<SQLQuerySymbolEntry> corresponding = r.collectColumnNameList(childNode);
                    SQLQueryRowsSourceModel nextSource = cc.get(i);
                    Interval range = Interval.of(n.getRealInterval().a, childNode.getRealInterval().b);
                    SQLQueryRowsSetCorrespondingOperationKind opKind = switch (childNode.getNodeKindId()) {
                        case SQLStandardParser.RULE_intersectTerm ->
                            SQLQueryRowsSetCorrespondingOperationKind.INTERSECT;
                        default ->
                            throw new UnsupportedOperationException("Unexpected child node kind at nonJoinQueryTerm");
                    };
                    source = new SQLQueryRowsSetCorrespondingOperationModel(r, range, childNode, source, nextSource, corresponding, opKind);
                }
                return source;
            }
        },
        STMKnownRuleNames.joinedTable, (n, cc, r) -> {
            // joinedTable: (nonjoinedTableReference|(LeftParen joinedTable RightParen)) (naturalJoinTerm|crossJoinTerm)+;
            if (cc.isEmpty()) {
                return r.getQueryDataContext().getDefaultTable(n);
            } else {
                SQLQueryRowsSourceModel source = cc.get(0);
                for (int i = 1; i < cc.size(); i++) {
                    final SQLQueryRowsSourceModel currSource = source;
                    final SQLQueryRowsSourceModel nextSource = cc.get(i);
                    // TODO see second case of the first source if parens are correctly ignored here
                    STMTreeNode childNode = n.getStmChild(i);
                    Interval range = Interval.of(n.getRealInterval().a, childNode.getRealInterval().b);
                    source = switch (childNode.getNodeKindId()) {
                        case SQLStandardParser.RULE_naturalJoinTerm -> {
                            Optional<STMTreeNode> joinConditionNode = Optional.ofNullable(childNode.findChildOfName(STMKnownRuleNames.joinSpecification))
                                    .map(cn -> cn.findChildOfName(STMKnownRuleNames.joinCondition));
                            if (joinConditionNode.isPresent()) {
                                try (SQLQueryModelContext.LexicalScopeHolder condScope = r.openScope()) {
                                    condScope.lexicalScope.registerSyntaxNode(joinConditionNode.get());
                                    yield joinConditionNode.map(cn -> cn.findChildOfName(STMKnownRuleNames.searchCondition))
                                            .map(r::collectValueExpression)
                                            .map(e -> new SQLQueryRowsNaturalJoinModel(r, range, childNode, currSource, nextSource, e, condScope.lexicalScope))
                                            .orElseGet(() -> new SQLQueryRowsNaturalJoinModel(r, range, childNode, currSource, nextSource, Collections.emptyList()));
                                }
                            } else {
                                yield new SQLQueryRowsNaturalJoinModel(r, range, childNode, currSource, nextSource, r.collectColumnNameList(childNode));
                            }
                        }
                        case SQLStandardParser.RULE_crossJoinTerm ->
                            new SQLQueryRowsCrossJoinModel(r, range, childNode, currSource, nextSource);
                        default ->
                            throw new UnsupportedOperationException("Unexpected child node kind at queryExpression");
                    };
                }
                return source;
            }
        },
        STMKnownRuleNames.fromClause, (n, cc, r) -> {
            if (cc.isEmpty()) {
                return r.getQueryDataContext().getDefaultTable(n);
            } else {
                SQLQueryRowsSourceModel source = cc.get(0);
                for (int i = 1; i < cc.size(); i++) {
                    STMTreeNode childNode = n.getStmChild(1 + i * 2);
                    SQLQueryRowsSourceModel nextSource = cc.get(i);
                    Interval range = Interval.of(n.getRealInterval().a, childNode.getRealInterval().b);
                    source = switch (childNode.getNodeKindId()) {
                        case SQLStandardParser.RULE_tableReference ->
                            new SQLQueryRowsCrossJoinModel(r, range, childNode, source, nextSource);
                        default -> throw new UnsupportedOperationException("Unexpected child node kind at fromClause");
                    };
                }
                return source;
            }
        },
        STMKnownRuleNames.querySpecification, (n, cc, r) -> {
            STMTreeNode selectListNode = n.findChildOfName(STMKnownRuleNames.selectList);
            SQLQuerySelectionResultModel resultModel = new SQLQuerySelectionResultModel(
                selectListNode, (selectListNode.getChildCount() + 1) / 2
            );

            SQLQueryLexicalScope selectListScope;
            STMTreeNode selectKeywordNode;
            try (SQLQueryModelContext.LexicalScopeHolder selectListScopeHolder = r.openScope()) {
                selectListScope = selectListScopeHolder.lexicalScope;
                selectKeywordNode = n.getStmChild(0); // SELECT keyword

                for (int i = 0; i < selectListNode.getChildCount(); i += 2) {
                    STMTreeNode selectSublist = selectListNode.getStmChild(i);
                    if (selectSublist.getChildCount() > 0) {
                        STMTreeNode sublistNode = selectSublist.getStmChild(0);
                        if (sublistNode != null) {
                            switch (sublistNode.getNodeKindId()) { // selectSublist: (Asterisk|derivedColumn|qualifier Period Asterisk
                                case SQLStandardParser.RULE_derivedColumn -> {
                                    // derivedColumn: valueExpression (asClause)?; asClause: (AS)? columnName;
                                    SQLQueryValueExpression expr = r.collectValueExpression(sublistNode.getStmChild(0));
                                    if (expr instanceof SQLQueryValueTupleReferenceExpression tupleRef) {
                                        resultModel.addTupleSpec(sublistNode, tupleRef);
                                    } else {
                                        if (sublistNode.getChildCount() > 1) {
                                            STMTreeNode asClause = sublistNode.getStmChild(1);
                                            SQLQuerySymbolEntry asColumnName = r.collectIdentifier(
                                                asClause.getStmChild(asClause.getChildCount() - 1)
                                            );
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
            }

            SQLQueryRowsSourceModel source = cc.isEmpty() ? r.getQueryDataContext().getDefaultTable(n) : cc.get(0);
            STMTreeNode tableExpr = n.findChildOfName(STMKnownRuleNames.tableExpression);
            SQLQueryRowsProjectionModel projectionModel;
            if (tableExpr != null) {
                STMTreeNode fromKeywordNode = tableExpr.getStmChild(0);
                selectListScope.setInterval(Interval.of(selectKeywordNode.getRealInterval().a, fromKeywordNode.getRealInterval().b));

                SQLQueryLexicalScope fromScope = new SQLQueryLexicalScope();

                STMTreeNode[] filterNodes = new STMTreeNode[]{
                    tableExpr.findChildOfName(STMKnownRuleNames.whereClause),
                    tableExpr.findChildOfName(STMKnownRuleNames.groupByClause),
                    tableExpr.findChildOfName(STMKnownRuleNames.havingClause),
                    tableExpr.findChildOfName(STMKnownRuleNames.orderByClause)
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
                            try (SQLQueryModelContext.LexicalScopeHolder exprScope = r.openScope()) {
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
                    r, n, selectListScope, source, fromScope,
                    SQLQueryRowsProjectionModel.FiltersData.of(filterExprs[0], filterExprs[1], filterExprs[2], filterExprs[3]),
                    SQLQueryRowsProjectionModel.FiltersData.of(scopes[1], scopes[2], scopes[3], scopes[4]),
                    resultModel
                );
            } else {
                projectionModel = new SQLQueryRowsProjectionModel(r, n, selectListScope, source, resultModel);
            }
            return projectionModel;
        },
        STMKnownRuleNames.nonjoinedTableReference, (n, cc, r) -> {
            // can they both be missing?
            SQLQueryRowsSourceModel source;
            if (cc.isEmpty()) {
                STMTreeNode tableNameNode = n.findChildOfName(STMKnownRuleNames.tableName);
                if (tableNameNode != null) {
                    source = r.collectTableReference(tableNameNode);
                } else {
                    source = r.getQueryDataContext().getDefaultTable(n);
                }
            } else {
                source = cc.get(0);
            }
            // TODO column reference at PARTITION clause
            if (n.getChildCount() > 1) {
                STMTreeNode lastSubnode = n.getStmChild(n.getChildCount() - 1);
                if (lastSubnode.getNodeName().equals(STMKnownRuleNames.correlationSpecification)) {
                    SQLQuerySymbolEntry correlationName = r.collectIdentifier(
                        lastSubnode.getStmChild(lastSubnode.getChildCount() == 1 || lastSubnode.getChildCount() == 4 ? 0 : 1)
                    );
                    source = new SQLQueryRowsCorrelatedSourceModel(r, n, source, correlationName, r.collectColumnNameList(lastSubnode));
                }
            }
            return source;
        },
        STMKnownRuleNames.explicitTable, (n, cc, r) -> r.collectTableReference(n),
        STMKnownRuleNames.tableValueConstructor, (n, cc, r) -> {
            List<SQLQueryValueExpression> values = new ArrayList<>(n.getChildCount() / 2 + 1); // values separated by comma
            for (int i = 1; i < n.getChildCount(); i += 2) {
                values.add(r.collectValueExpression(n.getStmChild(i)));
            }
            return new SQLQueryRowsTableValueModel(r, n, values);
        }
    );

}
