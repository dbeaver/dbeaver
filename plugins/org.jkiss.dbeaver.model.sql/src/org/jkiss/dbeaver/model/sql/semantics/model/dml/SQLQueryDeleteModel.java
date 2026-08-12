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
import org.jkiss.dbeaver.model.sql.semantics.*;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsSourceContext;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryModelContent;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.sql.semantics.model.expressions.SQLQueryValueExpression;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsCorrelatedSourceModel;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsSourceModel;
import org.jkiss.dbeaver.model.stm.STMKnownRuleNames;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

import java.util.Collections;
import java.util.List;

/**
 * Describes DELETE statement
 */
public class SQLQueryDeleteModel extends SQLQueryModelContent {

    @Nullable
    private final SQLQueryValueExpression whereClause;
    @Nullable
    private final SQLQueryRowsSourceModel rowsSource;
    @Nullable
    private final SQLQueryLexicalScope sourceScope;
    @Nullable
    private final SQLQueryLexicalScope conditionsScope;
    @Nullable
    private final SQLQueryLexicalScope tailScope;

    private SQLQueryDeleteModel(
        @NotNull STMTreeNode syntaxNode,
        @Nullable SQLQueryRowsSourceModel rowsSource,
        @Nullable SQLQueryValueExpression whereClause,
        @Nullable SQLQueryLexicalScope sourceScope,
        @Nullable SQLQueryLexicalScope conditionsScope,
        @Nullable SQLQueryLexicalScope tailScope
    ) {
        super(syntaxNode.getRealInterval(), syntaxNode, rowsSource);
        this.whereClause = whereClause;
        this.rowsSource = rowsSource;
        this.sourceScope = sourceScope;
        this.conditionsScope = conditionsScope;
        this.tailScope = tailScope;
    }

    @NotNull
    public static SQLQueryModelContent recognize(@NotNull SQLQueryModelRecognizer recognizer, @NotNull STMTreeNode node) {
        STMTreeNode tableNameNode = node.findFirstChildOfName(STMKnownRuleNames.tableName);
        STMTreeNode aliasNode = node.findFirstChildOfName(STMKnownRuleNames.correlationName);
        STMTreeNode whereClauseNode = node.findFirstChildOfName(STMKnownRuleNames.whereClause);

        SQLQueryRowsSourceModel rowsSource;
        SQLQueryLexicalScope sourceScope;
        try (SQLQueryModelRecognizer.LexicalScopeHolder holder = recognizer.openScope()) {
            sourceScope = holder.lexicalScope;
            rowsSource = tableNameNode == null ? null : recognizer.collectTableReference(tableNameNode, false);

            List<STMTreeNode> immediateChildren = node.getChildren();
            int from = tableNameNode != null ? immediateChildren.get(immediateChildren.indexOf(tableNameNode) - 1).getRealInterval().b + 2
                : immediateChildren.size() <= 2 ? immediateChildren.get(immediateChildren.size() - 1).getRealInterval().b + 2
                : 0;
            int to = tableNameNode != null ? tableNameNode.getRealInterval().b
                : aliasNode != null ? aliasNode.getRealInterval().a - 1
                : Integer.MAX_VALUE;
            sourceScope.setInterval(Interval.of(from, to));
        }

        SQLQuerySymbolEntry alias = aliasNode == null ? null : recognizer.collectIdentifier(aliasNode, null);

        if (alias != null && rowsSource != null) {
            Interval correlatedRegion = Interval.of(rowsSource.getInterval().a, alias.getInterval().b);
            rowsSource = new SQLQueryRowsCorrelatedSourceModel(node, rowsSource, alias, Collections.emptyList());
        }

        SQLQueryLexicalScope tailScope;
        SQLQueryLexicalScope conditionsScope;
        SQLQueryValueExpression whereClauseExpr;
        if (whereClauseNode != null) {
            try (SQLQueryModelRecognizer.LexicalScopeHolder holder = recognizer.openScope()) {
                conditionsScope = holder.lexicalScope;
                whereClauseExpr = recognizer.collectValueExpression(whereClauseNode, conditionsScope);
            }
            STMTreeNode lastConditionKwNode =  whereClauseNode.findFirstNonErrorChild();
            int from = lastConditionKwNode != null ? lastConditionKwNode.getRealInterval().b + 2 : whereClauseNode.getRealInterval().a;
            int to = Integer.MAX_VALUE;
            conditionsScope.setInterval(Interval.of(from, to));
            tailScope = conditionsScope;
        } else {
            whereClauseExpr = null;
            conditionsScope = null;
            tailScope = sourceScope;
        }

        return new SQLQueryDeleteModel(node, rowsSource, whereClauseExpr, sourceScope, conditionsScope, tailScope);
    }

    @Nullable
    public SQLQueryValueExpression getCondition() {
        return this.whereClause;
    }
    
    @Nullable
    public SQLQueryRowsSourceModel getRowsSource() {
        return this.rowsSource;
    }

    @Override
    public void resolveObjectAndRowsReferences(@NotNull SQLQueryRowsSourceContext context, @NotNull SQLQueryRecognitionContext statistics) {
        if (this.rowsSource != null) {
            context = this.rowsSource.resolveRowSources(context, statistics);
        }

        if (this.sourceScope != null) {
            this.sourceScope.setSymbolsOrigin(new SQLQuerySymbolOrigin.RowsSourceRef(context));
        }

        if (this.whereClause != null) {
            this.whereClause.resolveRowSources(context, statistics);
        }
    }

    @Override
    public void resolveValueRelations(@NotNull SQLQueryRowsDataContext context, @NotNull SQLQueryRecognitionContext statistics) {
        SQLQueryRowsDataContext rowsContext;
        if (this.rowsSource != null) {
            this.rowsSource.resolveValueRelations(context, statistics);
            rowsContext = this.rowsSource.getRowsDataContext();
        } else {
            rowsContext = context;
        }

        if (this.whereClause != null) {
            this.whereClause.resolveValueRelations(rowsContext, statistics);
        }

        if (this.conditionsScope != null) {
            this.conditionsScope.setSymbolsOrigin(new SQLQuerySymbolOrigin.RowsDataRef(rowsContext));
        }

        if (this.tailScope != null) {
            this.setTailOrigin(this.tailScope.getSymbolsOrigin());
        }
    }

    @Nullable
    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
        return visitor.visitTableStatementDelete(this, arg);
    }
}
