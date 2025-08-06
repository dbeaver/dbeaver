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
package org.jkiss.dbeaver.model.sql.semantics.model.dml;

import org.antlr.v4.runtime.misc.Interval;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardParser;
import org.jkiss.dbeaver.model.sql.semantics.*;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsSourceContext;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryModelContent;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.sql.semantics.model.expressions.SQLQueryValueColumnReferenceExpression;
import org.jkiss.dbeaver.model.sql.semantics.model.expressions.SQLQueryValueExpression;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsSourceModel;
import org.jkiss.dbeaver.model.stm.STMKnownRuleNames;
import org.jkiss.dbeaver.model.stm.STMTreeNode;
import org.jkiss.dbeaver.model.stm.STMUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Describes UPDATE statement
 */
public class SQLQueryUpdateModel extends SQLQueryModelContent {
    @Nullable
    private final SQLQueryRowsSourceModel targetRows;
    @Nullable
    private final List<SQLQueryUpdateSetClauseModel> setClauseList;
    @Nullable
    private final SQLQueryRowsSourceModel sourceRows;
    @Nullable
    private final SQLQueryValueExpression whereClause;
    @Nullable
    private final SQLQueryValueExpression orderByClause;
    @Nullable
    private final SQLQueryLexicalScope targetsScope;
    @Nullable
    private final SQLQueryLexicalScope conditionsScope;
    @Nullable
    private final SQLQueryLexicalScope tailScope;

    public SQLQueryUpdateModel(
        @NotNull STMTreeNode syntaxNode,
        @Nullable SQLQueryRowsSourceModel targetRows,
        @Nullable List<SQLQueryUpdateSetClauseModel> setClauseList,
        @Nullable SQLQueryRowsSourceModel sourceRows,
        @Nullable SQLQueryValueExpression whereClause,
        @Nullable SQLQueryValueExpression orderByClause,
        @Nullable SQLQueryLexicalScope targetsScope,
        @Nullable SQLQueryLexicalScope conditionsScope,
        @Nullable SQLQueryLexicalScope tailScope
    ) {
        super(syntaxNode.getRealInterval(), syntaxNode);
        this.targetRows = targetRows;
        this.setClauseList = setClauseList;
        this.sourceRows = sourceRows;
        this.whereClause = whereClause;
        this.orderByClause = orderByClause;
        this.targetsScope = targetsScope;
        this.conditionsScope = conditionsScope;
        this.tailScope = tailScope;

        if (targetsScope != null) {
            this.registerLexicalScope(targetsScope);
        }
        if (conditionsScope != null) {
            this.registerLexicalScope(conditionsScope);
        }
    }

    @NotNull
    public static SQLQueryModelContent recognize(@NotNull SQLQueryModelRecognizer recognizer, @NotNull STMTreeNode node) {
        STMTreeNode targetTableNode = node.findFirstChildOfName(STMKnownRuleNames.tableReference);
        SQLQueryRowsSourceModel targetSet = targetTableNode == null ? null : recognizer.collectQueryExpression(targetTableNode);

        SQLQueryLexicalScope tailScope;
        SQLQueryLexicalScope targetsScope;
        STMTreeNode setTermNode = node.findFirstChildOfName(STMKnownRuleNames.SET_TERM);
        Interval targetsScopeInterval = Interval.of(setTermNode != null
            ? setTermNode.getRealInterval().b + 2
            : Integer.MAX_VALUE, Integer.MAX_VALUE);

        List<SQLQueryUpdateSetClauseModel> setClauseList = new ArrayList<>();
        STMTreeNode setClauseListNode = node.findFirstChildOfName(STMKnownRuleNames.setClauseList);
        if (setClauseListNode != null) {
            try (SQLQueryModelRecognizer.LexicalScopeHolder holder = recognizer.openScope()) {
                targetsScope = holder.lexicalScope;
                for (STMTreeNode setClauseNode : setClauseListNode.findChildrenOfName(STMKnownRuleNames.setClause)) {
                    STMTreeNode setTargetNode = setClauseNode.findFirstNonErrorChild();
                    List<SQLQueryValueExpression> targets = setTargetNode == null
                        ? Collections.emptyList()
                        : switch (setTargetNode.getNodeKindId()) {
                            case SQLStandardParser.RULE_setTarget -> {
                                STMTreeNode targetNameNode = setTargetNode.findFirstNonErrorChild();
                                SQLQuerySymbolEntry targetName = targetNameNode == null ? null : recognizer.collectIdentifier(targetNameNode, null);
                                yield List.of(new SQLQueryValueColumnReferenceExpression(setTargetNode, targetName));
                            }
                            case SQLStandardParser.RULE_setTargetList ->
                                STMUtils.expandSubtree(
                                    setTargetNode,
                                    Set.of(STMKnownRuleNames.setTargetList),
                                    Set.of(STMKnownRuleNames.valueReference)
                                ).stream().map(cn -> recognizer.collectValueExpression(cn, targetsScope)).collect(Collectors.toList());
                            case SQLStandardParser.RULE_anyUnexpected ->
                                // error in query text, ignoring it
                                Collections.emptyList();
                            default -> throw new UnsupportedOperationException(
                                "Set target list expected while facing with " + setTargetNode.getNodeName()
                            );
                        };
                    STMTreeNode updateSourceNode = setClauseNode.findFirstChildOfName(STMKnownRuleNames.updateSource);
                    List<SQLQueryValueExpression> sources = updateSourceNode == null
                        ? Collections.emptyList()
                        : STMUtils.expandSubtree(
                            updateSourceNode,
                            Set.of(STMKnownRuleNames.updateSource),
                            Set.of(STMKnownRuleNames.updateValue)
                        ).stream()
                        .map(STMTreeNode::findFirstNonErrorChild)
                        .filter(Objects::nonNull)
                        .map(cn -> recognizer.collectValueExpression(cn, targetsScope))
                        .collect(Collectors.toList());
                    setClauseList.add(
                        new SQLQueryUpdateSetClauseModel(
                            setClauseNode,
                            targets,
                            sources,
                            setClauseNode.getTextContent()
                        )
                    );
                }
            }
        } else {
            targetsScope = null;
        }

        // updateStatement: UPDATE anyWordsWithProperty?? tableReference? (SET setClauseList? fromClause? whereClause? orderByClause? limitClause? anyWordsWithProperty??)?;

        STMTreeNode fromClauseNode = node.findFirstChildOfName(STMKnownRuleNames.fromClause);
        SQLQueryRowsSourceModel sourceSet = fromClauseNode == null ? null : recognizer.collectQueryExpression(fromClauseNode);

        SQLQueryValueExpression whereClauseExpr;
        SQLQueryValueExpression orderByExpr;

        STMTreeNode whereClauseNode = node.findFirstChildOfName(STMKnownRuleNames.whereClause);
        STMTreeNode orderByClauseNode = node.findFirstChildOfName(STMKnownRuleNames.orderByClause);
        STMTreeNode limitClauseNode = node.findFirstChildOfName(STMKnownRuleNames.limitClause);
        SQLQueryLexicalScope conditionsScope;

        if (whereClauseNode != null || orderByClauseNode != null) {
            try (SQLQueryModelRecognizer.LexicalScopeHolder holder = recognizer.openScope()) {
                conditionsScope = holder.lexicalScope;
                whereClauseExpr = whereClauseNode == null ? null : recognizer.collectValueExpression(whereClauseNode, conditionsScope);
                orderByExpr = orderByClauseNode == null ? null : recognizer.collectValueExpression(orderByClauseNode, conditionsScope);
            }
            STMTreeNode lastConditionKwNode =  (whereClauseNode != null ? whereClauseNode : orderByClauseNode).findFirstNonErrorChild();
            int from = lastConditionKwNode != null ? lastConditionKwNode.getRealInterval().b + 2 : whereClauseNode.getRealInterval().a;
            int to;
            if (limitClauseNode != null) {
                to = limitClauseNode.getRealInterval().a;
                tailScope = null;
            } else {
                to = Integer.MAX_VALUE;
                tailScope = conditionsScope;
            }
            conditionsScope.setInterval(Interval.of(from, to));
        } else {
            whereClauseExpr = null;
            orderByExpr = null;
            conditionsScope = null;
            tailScope = limitClauseNode == null ? targetsScope : null;
        }

        if (targetsScope != null) {
            STMTreeNode firstConditionNode = whereClauseNode != null
                ? whereClauseNode
                : orderByClauseNode != null ? orderByClauseNode : limitClauseNode != null ? limitClauseNode : null;
            if (firstConditionNode != null) {
                targetsScopeInterval.b = firstConditionNode.getRealInterval().a - 1;
            }
            targetsScope.setInterval(targetsScopeInterval);
        }

        return new SQLQueryUpdateModel(node, targetSet, setClauseList, sourceSet, whereClauseExpr, orderByExpr, targetsScope, conditionsScope, tailScope);
    }

    @Nullable
    public SQLQueryRowsSourceModel getTargetRows() {
        return targetRows;
    }

    @Nullable
    public List<SQLQueryUpdateSetClauseModel> getSetClauseList() {
        return setClauseList;
    }

    @Nullable
    public SQLQueryRowsSourceModel getSourceRows() {
        return sourceRows;
    }

    @Nullable
    public SQLQueryValueExpression getWhereClause() {
        return whereClause;
    }

    @Nullable
    public SQLQueryValueExpression getOrderByClause() {
        return orderByClause;
    }

    @Override
    public void resolveObjectAndRowsReferences(@NotNull SQLQueryRowsSourceContext context, @NotNull SQLQueryRecognitionContext statistics) {
        SQLQueryRowsSourceContext targetContext;
        if (this.targetRows != null) {
            targetContext = this.targetRows.resolveRowSources(context, statistics);

            if (this.setClauseList != null) {
                for (SQLQueryUpdateSetClauseModel updateSetClauseModel : this.setClauseList) {
                    // resolve target columns against target set
                    for (SQLQueryValueExpression valueExpression : updateSetClauseModel.targets) {
                        valueExpression.resolveRowSources(targetContext, statistics);
                    }
                }
            }
        } else {
            // leave target column names as unresolved
            targetContext = context;
        }

        SQLQueryRowsSourceContext sourceContext = this.sourceRows != null
            ? this.sourceRows.resolveRowSources(context, statistics)
            : context;

        if (targetContext != context || sourceContext != context) {
            context = targetContext.combine(sourceContext);
        }

        if (this.setClauseList != null) {
            for (SQLQueryUpdateSetClauseModel setClauseModel : this.setClauseList) {
                // resolve source value expressions against combined participating sets
                for (SQLQueryValueExpression valueExpression : setClauseModel.sources) {
                    valueExpression.resolveRowSources(context, statistics);
                }
            }
        }

        if (this.whereClause != null) {
            this.whereClause.resolveRowSources(context, statistics);
        }
        if (this.orderByClause != null) {
            this.orderByClause.resolveRowSources(context, statistics);
        }
    }

    @Override
    public void resolveValueRelations(@NotNull SQLQueryRowsDataContext context, @NotNull SQLQueryRecognitionContext statistics) {
        SQLQueryRowsDataContext targetContext;
        if (this.targetRows != null) {
            this.targetRows.resolveValueRelations(context, statistics);
            targetContext = this.targetRows.getRowsDataContext();

            if (this.targetsScope != null) {
                this.targetsScope.setSymbolsOrigin(new SQLQuerySymbolOrigin.SyntaxBasedFromRowsData(targetContext));
            }
            if (this.setClauseList != null) {
                for (SQLQueryUpdateSetClauseModel updateSetClauseModel : this.setClauseList) {
                    // resolve target columns against target set
                    for (SQLQueryValueExpression valueExpression : updateSetClauseModel.targets) {
                        valueExpression.resolveValueRelations(targetContext, statistics);
                    }
                }
            }
        } else {
            // leave target column names as unresolved
            targetContext = context;
        }

        SQLQueryRowsDataContext sourceContext;
        if (this.sourceRows != null) {
            this.sourceRows.resolveValueRelations(context, statistics);
            sourceContext = this.sourceRows.getRowsDataContext();
        } else {
            sourceContext = context;
        }
        
        if (targetContext != context || sourceContext != context) {
            context = targetContext.combine(sourceContext);
        }
        
        if (this.setClauseList != null) {
            for (SQLQueryUpdateSetClauseModel setClauseModel : this.setClauseList) {
                // resolve source value expressions against combined participating sets
                for (SQLQueryValueExpression valueExpression : setClauseModel.sources) {
                    valueExpression.resolveValueRelations(context, statistics);
                }
            }
        }
        
        if (this.whereClause != null) {
            this.whereClause.resolveValueRelations(context, statistics);
        }
        if (this.orderByClause != null) {
            this.orderByClause.resolveValueRelations(context, statistics);
        }

        if (this.conditionsScope != null) {
            this.conditionsScope.setSymbolsOrigin(new SQLQuerySymbolOrigin.RowsDataRef(context));
        }

        if (this.tailScope != null) {
            this.setTailOrigin(this.tailScope.getSymbolsOrigin());
        }
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
        return visitor.visitTableStatementUpdate(this, arg);
    }
}
