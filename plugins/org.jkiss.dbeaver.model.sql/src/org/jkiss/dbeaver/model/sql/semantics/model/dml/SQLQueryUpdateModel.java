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
package org.jkiss.dbeaver.model.sql.semantics.model.dml;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardParser;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryModelRecognizer;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryModelContent;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
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
    private SQLQueryDataContext givenContext = null;
    @Nullable
    private SQLQueryDataContext resultContext = null;
    
    public SQLQueryUpdateModel(
        @NotNull STMTreeNode syntaxNode,
        @Nullable SQLQueryRowsSourceModel targetRows,
        @Nullable List<SQLQueryUpdateSetClauseModel> setClauseList,
        @Nullable SQLQueryRowsSourceModel sourceRows,
        @Nullable SQLQueryValueExpression whereClause,
        @Nullable SQLQueryValueExpression orderByClause
    ) {
        super(syntaxNode.getRealInterval(), syntaxNode);
        this.targetRows = targetRows;
        this.setClauseList = setClauseList;
        this.sourceRows = sourceRows;
        this.whereClause = whereClause;
        this.orderByClause = orderByClause;
    }

    @NotNull
    public static SQLQueryModelContent recognize(@NotNull SQLQueryModelRecognizer recognizer, @NotNull STMTreeNode node) {
        STMTreeNode targetTableNode = node.findFirstChildOfName(STMKnownRuleNames.tableReference);
        SQLQueryRowsSourceModel targetSet = targetTableNode == null ? null : recognizer.collectQueryExpression(targetTableNode);

        List<SQLQueryUpdateSetClauseModel> setClauseList = new ArrayList<>();
        STMTreeNode setClauseListNode = node.findFirstChildOfName(STMKnownRuleNames.setClauseList);
        if (setClauseListNode != null) {
            for (STMTreeNode setClauseNode : setClauseListNode.findChildrenOfName(STMKnownRuleNames.setClause)) {
                STMTreeNode setTargetNode = setClauseNode.findFirstNonErrorChild();
                List<SQLQueryValueExpression> targets = setTargetNode == null
                    ? Collections.emptyList()
                    : switch (setTargetNode.getNodeKindId()) {
                        case SQLStandardParser.RULE_setTarget -> {
                            STMTreeNode targetReferenceNode = setTargetNode.findFirstNonErrorChild();
                            yield targetReferenceNode == null
                                ? Collections.emptyList()
                                : List.of(recognizer.collectKnownValueExpression(targetReferenceNode));
                        }
                        case SQLStandardParser.RULE_setTargetList ->
                            STMUtils.expandSubtree(
                                setTargetNode,
                                Set.of(STMKnownRuleNames.setTargetList),
                                Set.of(STMKnownRuleNames.valueReference)
                            ).stream().map(recognizer::collectValueExpression).collect(Collectors.toList());
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
                    .map(recognizer::collectValueExpression)
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

        STMTreeNode fromClauseNode = node.findFirstChildOfName(STMKnownRuleNames.fromClause);
        SQLQueryRowsSourceModel sourceSet = fromClauseNode == null ? null : recognizer.collectQueryExpression(fromClauseNode);

        STMTreeNode whereClauseNode = node.findFirstChildOfName(STMKnownRuleNames.whereClause);
        SQLQueryValueExpression whereClauseExpr = whereClauseNode == null ? null : recognizer.collectValueExpression(whereClauseNode);

        STMTreeNode orderByClauseNode = node.findFirstChildOfName(STMKnownRuleNames.orderByClause);
        SQLQueryValueExpression orderByExpr = orderByClauseNode == null ? null : recognizer.collectValueExpression(orderByClauseNode);

        return new SQLQueryUpdateModel(node, targetSet, setClauseList, sourceSet, whereClauseExpr, orderByExpr);
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
    protected void applyContext(@NotNull SQLQueryDataContext context, @NotNull SQLQueryRecognitionContext statistics) {
        this.givenContext = context;
        SQLQueryDataContext targetContext;
        if (this.targetRows != null) {
            targetContext = this.targetRows.propagateContext(context, statistics);
            
            if (this.setClauseList != null) {
                for (SQLQueryUpdateSetClauseModel updateSetClauseModel : this.setClauseList) {
                    // resolve target columns against target set
                    for (SQLQueryValueExpression valueExpression : updateSetClauseModel.targets) {
                        valueExpression.propagateContext(targetContext, statistics);
                    }
                }
            }
        } else {
            // leave target column names as unresolved
            targetContext = context;
        }
        
        SQLQueryDataContext sourceContext = this.sourceRows != null ? this.sourceRows.propagateContext(context, statistics) : context;
        
        if (targetContext != context || sourceContext != context) {
            context = targetContext.combine(sourceContext);
        }
        
        if (this.setClauseList != null) {
            for (SQLQueryUpdateSetClauseModel setClauseModel : this.setClauseList) {
                // resolve source value expressions against combined participating sets
                for (SQLQueryValueExpression valueExpression : setClauseModel.sources) {
                    valueExpression.propagateContext(context, statistics);
                }
            }
        }
        
        if (this.whereClause != null) {
            this.whereClause.propagateContext(context, statistics);
        }
        if (this.orderByClause != null) {
            this.orderByClause.propagateContext(context, statistics);
        }
        
        this.resultContext = context;
    }

    @Override
    public SQLQueryDataContext getGivenDataContext() {
        return this.givenContext;
    }
    
    @Override
    public SQLQueryDataContext getResultDataContext() {
        return this.resultContext;
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
        return visitor.visitTableStatementUpdate(this, arg);
    }
}
