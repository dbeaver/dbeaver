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
package org.jkiss.dbeaver.model.sql.semantics.model.expressions;


import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.semantics.*;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryExprType;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryResultColumn;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsSourceContext;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.stm.STMTreeNode;


/**
 * Describes column reference specified by column nae and optionally table name
 */
public class SQLQueryValueColumnReferenceExpression extends SQLQueryValueExpression {
    @Nullable
    private final SQLQuerySymbolEntry columnName;
    @Nullable
    private SQLQueryResultColumn column = null;
    
    public SQLQueryValueColumnReferenceExpression(
        @NotNull STMTreeNode syntaxNode,
        @Nullable SQLQuerySymbolEntry columnName
    ) {
        super(syntaxNode);
        this.columnName = columnName;
    }

    @Nullable
    @Override
    public SQLQuerySymbolClass getAssociatedSymbolClass() {
        return SQLQuerySemanticUtils.getIdentifierSymbolClass(this.columnName);
    }

    @Nullable
    public SQLQuerySymbolEntry getColumnName() {
        return this.columnName;
    }

    @Nullable
    @Override
    public SQLQuerySymbol getColumnNameIfTrivialExpression() {
        return this.columnName == null ? null : this.columnName.getSymbol();
    }

    @Nullable
    @Override
    public SQLQueryResultColumn getColumnIfTrivialExpression() {
        return this.column;
    }

    @Override
    protected void resolveRowSourcesImpl(@NotNull SQLQueryRowsSourceContext context, @NotNull SQLQueryRecognitionContext statistics) {
    }

    @NotNull
    @Override
    protected SQLQueryExprType resolveValueTypeImpl(
        @NotNull SQLQueryRowsDataContext context,
        @NotNull SQLQueryRecognitionContext statistics
    ) {
        SQLQueryExprType type;
        SQLQueryResultColumn resultColumn;

        if (this.columnName != null) {
            SQLQuerySymbolOrigin columnRefOrigin = new SQLQuerySymbolOrigin.ColumnNameFromRowsData(context);
            resultColumn = context.resolveColumn(statistics.getMonitor(), columnName.getName());

            if (resultColumn != null || !context.getRowsSources().hasUnresolvedSource()) {
                SQLQuerySemanticUtils.propagateColumnDefinition(columnName, resultColumn, statistics, columnRefOrigin);
            }
            type = resultColumn != null ? resultColumn.type : SQLQueryExprType.UNKNOWN;

            if (columnName.getOrigin() == null) {
                columnName.setOrigin(columnRefOrigin);
            }
        } else {
            statistics.appendError(this.getSyntaxNode(), "Invalid column reference");
            resultColumn = null;
            type = this.type;
        }
        this.column = resultColumn;
        return type;
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
        return visitor.visitValueColumnRefExpr(this, arg);
    }

    @NotNull
    @Override
    public String toString() {
        String columnName = this.columnName == null ? "<NULL>" : this.columnName.getName();
        String type = this.type == null ? "<NULL>" : this.type.toString();
        return "ColumnReference[" + columnName + ":" + type + "]";
    }
}