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

import org.antlr.v4.runtime.misc.Interval;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryModelRecognizer;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolEntry;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryModelContent;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.sql.semantics.model.expressions.SQLQueryValueExpression;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsCorrelatedSourceModel;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsTableDataModel;
import org.jkiss.dbeaver.model.stm.STMKnownRuleNames;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

import java.util.Collections;

/**
 * Describes DELETE statement
 */
public class SQLQueryDeleteModel extends SQLQueryDMLStatementModel {

    @Nullable
    private final SQLQueryValueExpression whereClause;
    @Nullable 
    private final SQLQueryRowsCorrelatedSourceModel aliasedTableModel;

    @NotNull
    public static SQLQueryModelContent recognize(@NotNull SQLQueryModelRecognizer recognizer, @NotNull STMTreeNode node) {
        STMTreeNode tableNameNode = node.findFirstChildOfName(STMKnownRuleNames.tableName);
        SQLQueryRowsTableDataModel tableModel = tableNameNode == null ? null : recognizer.collectTableReference(tableNameNode, false);

        STMTreeNode aliasNode = node.findFirstChildOfName(STMKnownRuleNames.correlationName);
        SQLQuerySymbolEntry alias = aliasNode == null ? null : recognizer.collectIdentifier(aliasNode, null);

        STMTreeNode whereClauseNode = node.findFirstChildOfName(STMKnownRuleNames.whereClause);
        SQLQueryValueExpression whereClauseExpr = whereClauseNode == null ? null : recognizer.collectValueExpression(whereClauseNode);

        return new SQLQueryDeleteModel(node, tableModel, alias, whereClauseExpr);
    }

    private SQLQueryDeleteModel(
        @NotNull STMTreeNode syntaxNode,
        @Nullable SQLQueryRowsTableDataModel tableModel,
        @Nullable SQLQuerySymbolEntry alias,
        @Nullable SQLQueryValueExpression whereClause
    ) {
        super(syntaxNode, tableModel);
        this.whereClause = whereClause;
        
        if (alias != null && tableModel != null) {
            Interval correlatedRegion = Interval.of(tableModel.getInterval().a, alias.getInterval().b);
            this.aliasedTableModel = new SQLQueryRowsCorrelatedSourceModel(syntaxNode, tableModel, alias, Collections.emptyList());
        } else {
            this.aliasedTableModel  = null;
        }
    }

    @Nullable
    public SQLQueryValueExpression getCondition() {
        return this.whereClause;
    }
    
    @Nullable
    public SQLQueryRowsCorrelatedSourceModel getAliasedTableModel() {
        return this.aliasedTableModel;
    }
    
    @Override
    public void propagateContextImpl(@NotNull SQLQueryDataContext context, @NotNull SQLQueryRecognitionContext statistics) {
        if (this.aliasedTableModel != null) {
            context = this.aliasedTableModel.propagateContext(context, statistics);
        }
        if (this.whereClause != null) {
            this.whereClause.propagateContext(context, statistics);
        }
    }

    @Nullable
    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
        return visitor.visitTableStatementDelete(this, arg);
    }
}
