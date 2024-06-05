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
package org.jkiss.dbeaver.model.sql.semantics.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.semantics.*;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryExprType;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryResultColumn;
import org.jkiss.dbeaver.model.sql.semantics.context.SourceResolutionResult;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

/**
 * Describes column reference specified by column nae and optionally table name
 */
public class SQLQueryValueColumnReferenceExpression extends SQLQueryValueExpression {
    @Nullable
    private final SQLQueryQualifiedName tableName;
    @NotNull
    private final SQLQuerySymbolEntry columnName;
    @Nullable
    private SQLQueryResultColumn column = null;
    
    public SQLQueryValueColumnReferenceExpression(@NotNull STMTreeNode syntaxNode, @NotNull SQLQuerySymbolEntry columnName) {
        super(syntaxNode);
        this.tableName = null;
        this.columnName = columnName;
    }

    public SQLQueryValueColumnReferenceExpression(
        @NotNull STMTreeNode syntaxNode,
        @NotNull SQLQueryQualifiedName tableName,
        @NotNull SQLQuerySymbolEntry columnName
    ) {
        super(syntaxNode);
        this.tableName = tableName;
        this.columnName = columnName;
    }

    @Nullable
    public SQLQueryQualifiedName getTableName() {
        return this.tableName;
    }

    @NotNull
    @Override
    public SQLQuerySymbol getColumnNameIfTrivialExpression() {
        return this.columnName.getSymbol();
    }

    @Nullable
    @Override
    public SQLQueryResultColumn getColumnIfTrivialExpression() {
        return this.column;
    }

    /**
     * Propagate semantics context and establish relations through the query model for column definition
     */
    public static void propagateColumnDefinition(@NotNull SQLQuerySymbolEntry columnName, @Nullable SQLQueryResultColumn resultColumn, @NotNull SQLQueryRecognitionContext statistics) {
        // TODO consider ambiguity
        if (resultColumn != null) {
            columnName.setDefinition(resultColumn.symbol.getDefinition());
        } else {
            columnName.getSymbol().setSymbolClass(SQLQuerySymbolClass.ERROR);
            statistics.appendError(columnName, "Column not found in dataset");
        }
    }

    @Override
    protected void propagateContextImpl(@NotNull SQLQueryDataContext context, @NotNull SQLQueryRecognitionContext statistics) {
        SQLDialect dialect = context.getDialect();
        SQLQueryExprType type;
        if (this.tableName != null && this.tableName.isNotClassified() && this.columnName.isNotClassified()) {
            SourceResolutionResult rr = context.resolveSource(statistics.getMonitor(), this.tableName.toListOfStrings());
            if (rr != null) {
                this.tableName.setDefinition(rr);
                SQLQueryResultColumn resultColumn = rr.source.getResultDataContext().resolveColumn(statistics.getMonitor(), this.columnName.getName());
                propagateColumnDefinition(this.columnName, resultColumn, statistics);
                this.column = resultColumn;
                type = resultColumn != null ? resultColumn.type : SQLQueryExprType.UNKNOWN;
            } else {
                this.tableName.setSymbolClass(SQLQuerySymbolClass.ERROR);
                statistics.appendError(this.tableName.entityName, "Table or subquery not found");
                type = SQLQueryExprType.UNKNOWN;
            }
        } else if (this.tableName == null && this.columnName.isNotClassified()) {
            SQLQueryResultColumn resultColumn = context.resolveColumn(statistics.getMonitor(), this.columnName.getName());

            SQLQuerySymbolClass forcedClass = null;
            if (resultColumn == null) {
                String rawString = columnName.getRawName();
                if (dialect.isQuotedString(rawString)) {
                    forcedClass = SQLQuerySymbolClass.STRING;
                } else {
                    forcedClass = SQLQueryModelRecognizer.tryFallbackSymbolForStringLiteral(dialect, this.columnName, resultColumn != null);
                }
            } else {
                this.column = resultColumn;
            }

            if (forcedClass != null) {
                this.columnName.getSymbol().setSymbolClass(forcedClass);
                type = forcedClass == SQLQuerySymbolClass.STRING ? SQLQueryExprType.STRING : SQLQueryExprType.UNKNOWN;
            } else {
                propagateColumnDefinition(this.columnName, resultColumn, statistics);
                type = resultColumn != null ? resultColumn.type : SQLQueryExprType.UNKNOWN;
            }
        } else {
            type = SQLQueryExprType.UNKNOWN;
        }
        this.type = type;
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
        return visitor.visitValueColumnRefExpr(this, arg);
    }
    
    @Override
    public String toString() {
        String name = this.tableName == null
            ? this.columnName.getName()
            : this.tableName.toIdentifierString() + "." + this.columnName.getName();
        String type = this.type == null ? "<NULL>" : this.type.toString();
        return "ColumnReference[" + name + ":" + type + "]";
    }
}