/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardLexer;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardParser;
import org.jkiss.dbeaver.model.sql.semantics.*;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsSourceContext;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryModelContent;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.sql.semantics.model.expressions.SQLQueryValueExpression;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsSourceModel;
import org.jkiss.dbeaver.model.stm.STMKnownRuleNames;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

import java.util.*;
import java.util.stream.Stream;

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
    @NotNull
    private final List<SQLQueryLexicalScope> targetGapScopes;
    @Nullable
    private final SQLQueryLexicalScope conditionsScope;
    @Nullable
    private final SQLQueryLexicalScope tailScope;

    private SQLQueryUpdateModel(
        @NotNull STMTreeNode syntaxNode,
        @Nullable SQLQueryRowsSourceModel targetRows,
        @Nullable List<SQLQueryUpdateSetClauseModel> setClauseList,
        @Nullable SQLQueryRowsSourceModel sourceRows,
        @Nullable SQLQueryValueExpression whereClause,
        @Nullable SQLQueryValueExpression orderByClause,
        @NotNull List<SQLQueryLexicalScope> targetGapScopes,
        @Nullable SQLQueryLexicalScope conditionsScope,
        @Nullable SQLQueryLexicalScope tailScope
    ) {
        super(syntaxNode.getRealInterval(), syntaxNode);
        this.targetRows = targetRows;
        this.setClauseList = setClauseList;
        this.sourceRows = sourceRows;
        this.whereClause = whereClause;
        this.orderByClause = orderByClause;
        this.targetGapScopes = targetGapScopes;
        this.conditionsScope = conditionsScope;
        this.tailScope = tailScope;

        if (setClauseList != null) {
            for (SQLQueryUpdateSetClauseModel clause : setClauseList) {
                this.registerSubnode(clause);
            }
        }
        // Parsed `SET a.BAT=1`/`SET @value=1` targets own their symbols; only missing targets use UPDATE gaps.
        for (SQLQueryLexicalScope scope : targetGapScopes) {
            this.registerLexicalScope(scope);
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
        STMTreeNode setTermNode = node.findFirstChildOfName(STMKnownRuleNames.SET_TERM);
        STMTreeNode fromClauseNode = node.findFirstChildOfName(STMKnownRuleNames.fromClause);
        STMTreeNode whereClauseNode = node.findFirstChildOfName(STMKnownRuleNames.whereClause);
        STMTreeNode orderByClauseNode = node.findFirstChildOfName(STMKnownRuleNames.orderByClause);
        STMTreeNode limitClauseNode = node.findFirstChildOfName(STMKnownRuleNames.limitClause);
        // Inclusive caret offsets: `SET x=1| WHERE` is still in SET; `SET x=1 |WHERE` starts the keyword.
        // No following clause means the last editor scope remains open past the parsed text.
        int endOfAssignments = Stream.of(fromClauseNode, whereClauseNode, orderByClauseNode, limitClauseNode)
            .filter(Objects::nonNull).mapToInt(n -> n.getRealInterval().a - 1).min().orElse(Integer.MAX_VALUE);
        // Missing SET targets end on the caret before the next clause, not on its keyword.
        int endOfTargetGap = endOfAssignments;

        List<SQLQueryUpdateSetClauseModel> setClauseList = new ArrayList<>();
        List<SQLQueryLexicalScope> targetGapScopes = new ArrayList<>();
        STMTreeNode setClauseListNode = node.findFirstChildOfName(STMKnownRuleNames.setClauseList);
        String commaTerm = SQLStandardLexer.VOCABULARY.getSymbolicName(SQLStandardLexer.Comma);
        List<STMTreeNode> children = setClauseListNode == null ? Collections.emptyList() : setClauseListNode.getChildren();
        List<STMTreeNode> clauses = setClauseListNode == null
            ? Collections.emptyList() : setClauseListNode.findChildrenOfName(STMKnownRuleNames.setClause);
        if (setTermNode != null && (clauses.isEmpty() || !hasSetTarget(clauses.getFirst()))) {
            // `SET|` with no parsed target starts here and ends at the next assignment comma or statement clause.
            STMTreeNode nextComma = findNextComma(children, 0, commaTerm);
            addTargetGap(targetGapScopes, setTermNode.getRealInterval().b + 1,
                nextComma == null ? endOfTargetGap : Math.min(endOfTargetGap, nextComma.getRealInterval().a),
                clauses.isEmpty() ? null : clauses.getFirst(), nextComma != null);
        }
        if (setClauseListNode != null) {
            // `SET a=1, b=2` separates clauses; commas in `SET (a,b)=(1,2)` stay inside one clause.
            for (int i = 0; i < children.size(); i++) {
                STMTreeNode child = children.get(i);
                if (STMKnownRuleNames.setClause.equals(child.getNodeName())) {
                    // `SET a=1|, b=2`: the preceding clause owns the caret at comma.a, not after it.
                    int end = endOfAssignments;
                    if (i + 1 < children.size() && commaTerm.equals(children.get(i + 1).getNodeName())) {
                        end = children.get(i + 1).getRealInterval().a;
                    }
                    setClauseList.add(SQLQueryUpdateSetClauseModel.recognize(recognizer, child, end));
                } else if (commaTerm.equals(child.getNodeName())) {
                    STMTreeNode nextClause = i + 1 < children.size() ? children.get(i + 1) : null;
                    if (!hasSetTarget(nextClause)) {
                        // `SET a=1,|  , c=3`: with no `=`, only target completion applies up to the next comma.
                        STMTreeNode nextComma = findNextComma(children, i + 1, commaTerm);
                        addTargetGap(targetGapScopes, child.getRealInterval().b + 1,
                            nextComma == null ? endOfTargetGap : Math.min(endOfTargetGap, nextComma.getRealInterval().a),
                            nextClause, nextComma != null);
                    }
                }
            }
        }

        // updateStatement: UPDATE anyWordsWithProperty?? tableReference? (SET setClauseList? fromClause? whereClause? orderByClause? limitClause? anyWordsWithProperty??)?;

        SQLQueryRowsSourceModel sourceSet = fromClauseNode == null ? null : recognizer.collectQueryExpression(fromClauseNode);

        SQLQueryValueExpression whereClauseExpr;
        SQLQueryValueExpression orderByExpr;

        SQLQueryLexicalScope conditionsScope;

        if (whereClauseNode != null || orderByClauseNode != null) {
            try (SQLQueryModelRecognizer.LexicalScopeHolder holder = recognizer.openScope()) {
                conditionsScope = holder.lexicalScope;
                whereClauseExpr = whereClauseNode == null ? null : recognizer.collectValueExpression(whereClauseNode, conditionsScope);
                orderByExpr = orderByClauseNode == null ? null : recognizer.collectValueExpression(orderByClauseNode, conditionsScope);
            }
            // Start after `WHERE`/`ORDER`; bound at `LIMIT`'s start or leave the tail open for typing.
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
            if (limitClauseNode != null || fromClauseNode != null) {
                tailScope = null;
            } else {
                // At `SET a=1,|` EOF, the empty target gap wins over the preceding RHS scope.
                SQLQueryLexicalScope clauseTail = setClauseList.isEmpty() ? null : setClauseList.getLast().getLastScope();
                SQLQueryLexicalScope gapTail = targetGapScopes.isEmpty() ? null : targetGapScopes.getLast();
                tailScope = gapTail != null && (clauseTail == null ||
                    gapTail.getInterval().a > setClauseList.getLast().getInterval().b) ? gapTail : clauseTail;
            }
        }
        return new SQLQueryUpdateModel(node, targetSet, setClauseList, sourceSet, whereClauseExpr, orderByExpr,
            targetGapScopes, conditionsScope, tailScope);
    }

    private static boolean hasSetTarget(@Nullable STMTreeNode clause) {
        return clause != null && (clause.findFirstChildOfName(STMKnownRuleNames.setTarget) != null ||
            clause.findFirstChildOfName(STMKnownRuleNames.setTargetList) != null);
    }

    @Nullable
    private static STMTreeNode findNextComma(@NotNull List<STMTreeNode> children, int from, @NotNull String commaTerm) {
        for (int i = from; i < children.size(); i++) {
            if (commaTerm.equals(children.get(i).getNodeName())) {
                return children.get(i);
            }
        }
        return null;
    }

    private static void addTargetGap(
        @NotNull List<SQLQueryLexicalScope> gaps, int start, int end, @Nullable STMTreeNode followingClause,
        boolean boundedByComma
    ) {
        if (followingClause != null) {
            STMTreeNode first = followingClause.findFirstNonErrorChild();
            // Between assignment commas even unknown text is target-side; otherwise stop before unknown text/keywords.
            if (!boundedByComma && first != null && first.getNodeKindId() == SQLStandardParser.RULE_anyUnexpected &&
                !first.getTextContent().isEmpty()) {
                end = Math.min(end, first.getRealInterval().a - 1);
            }
            // A missing target never extends past `=` into the RHS scope.
            STMTreeNode equals = followingClause.findFirstChildOfName(
                SQLStandardLexer.VOCABULARY.getSymbolicName(SQLStandardLexer.EqualsOperator));
            if (equals != null) {
                end = Math.min(end, equals.getRealInterval().a - 1);
            }
        }
        // Adjacent tokens can leave no caret position in the gap; avoid registering an inverted interval.
        if (start <= end) {
            SQLQueryLexicalScope gap = new SQLQueryLexicalScope();
            gap.setInterval(Interval.of(start, end));
            gaps.add(gap);
        }
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
    public boolean tryResolveValueRelations(@NotNull SQLQueryRowsDataContext context, @NotNull SQLQueryRecognitionContext statistics) {
        SQLQueryRowsDataContext targetContext;
        if (this.targetRows != null) {
            if (!this.targetRows.tryResolveValueRelations(context, statistics)) {
                return false;
            }
            targetContext = this.targetRows.getRowsDataContext();

            // `UPDATE t a JOIN u b SET |` draws target proposals from both updated row sources.
            SQLQuerySymbolOrigin.UpdateTarget targetOrigin = new SQLQuerySymbolOrigin.UpdateTarget(targetContext);
            for (SQLQueryLexicalScope scope : this.targetGapScopes) {
                scope.setSymbolsOrigin(targetOrigin);
            }
            if (this.setClauseList != null) {
                for (SQLQueryUpdateSetClauseModel updateSetClauseModel : this.setClauseList) {
                    SQLQueryLexicalScope targetScope = updateSetClauseModel.getTargetScope();
                    if (targetScope != null) {
                        targetScope.setSymbolsOrigin(targetOrigin);
                    }
                    // resolve target columns against target set
                    for (SQLQueryValueExpression valueExpression : updateSetClauseModel.targets) {
                        if (!valueExpression.tryResolveValueRelations(targetContext, statistics)) {
                            return false;
                        }
                    }
                }
            }
        } else {
            // leave target column names as unresolved
            targetContext = context;
        }

        SQLQueryRowsDataContext sourceContext;
        if (this.sourceRows != null) {
            if (!this.sourceRows.tryResolveValueRelations(context, statistics)) {
                return false;
            }
            sourceContext = this.sourceRows.getRowsDataContext();
        } else {
            sourceContext = context;
        }
        
        if (targetContext != context || sourceContext != context) {
            context = targetContext.combine(sourceContext);
        }

        // `SET a.BAT = | FROM u` uses the combined target/FROM context, unlike the target before `=`.
        SQLQuerySymbolOrigin.RowsDataRef sourceOrigin = new SQLQuerySymbolOrigin.RowsDataRef(context);
        if (this.setClauseList != null) {
            for (SQLQueryUpdateSetClauseModel setClauseModel : this.setClauseList) {
                SQLQueryLexicalScope sourceScope = setClauseModel.getSourceScope();
                if (sourceScope != null) {
                    sourceScope.setSymbolsOrigin(sourceOrigin);
                }
                // resolve source value expressions against combined participating sets
                for (SQLQueryValueExpression valueExpression : setClauseModel.sources) {
                    if (!valueExpression.tryResolveValueRelations(context, statistics)) {
                        return false;
                    }
                }
            }
        }
        
        if (this.whereClause != null) {
            if (!this.whereClause.tryResolveValueRelations(context, statistics)) {
                return false;
            }
        }
        if (this.orderByClause != null) {
            if (!this.orderByClause.tryResolveValueRelations(context, statistics)) {
                return false;
            }
        }

        if (this.conditionsScope != null) {
            this.conditionsScope.setSymbolsOrigin(sourceOrigin);
        }

        if (this.tailScope != null) {
            this.setTailOrigin(this.tailScope.getSymbolsOrigin());
        }
        return true;
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
        return visitor.visitTableStatementUpdate(this, arg);
    }
}
