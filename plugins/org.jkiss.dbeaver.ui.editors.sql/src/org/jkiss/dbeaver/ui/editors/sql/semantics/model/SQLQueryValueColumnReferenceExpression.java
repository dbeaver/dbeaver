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
package org.jkiss.dbeaver.ui.editors.sql.semantics.model;

import org.antlr.v4.runtime.misc.Interval;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.ui.editors.sql.semantics.*;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SQLQueryExprType;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SourceResolutionResult;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SQLQueryResultTupleContext.SQLQueryResultColumn;

public class SQLQueryValueColumnReferenceExpression extends SQLQueryValueExpression {
    private final SQLQueryQualifiedName tableName;
    private final SQLQuerySymbolEntry columnName;
    
    private SQLQueryResultColumn column = null;
    
    public SQLQueryValueColumnReferenceExpression(@NotNull Interval range, @NotNull SQLQuerySymbolEntry columnName) {
        super(range);
        this.tableName = null;
        this.columnName = columnName;
    }

    public SQLQueryValueColumnReferenceExpression(
        @NotNull Interval range,
        @NotNull SQLQueryQualifiedName tableName,
        @NotNull SQLQuerySymbolEntry columnName
    ) {
        super(range);
        this.tableName = tableName;
        this.columnName = columnName;
    }

    public @Nullable SQLQueryQualifiedName getTableName() {
        return this.tableName;
    }

    @NotNull
    @Override
    public SQLQuerySymbol getColumnNameIfTrivialExpression() {
        return this.columnName.getSymbol();
    }
    
    @Override
    public SQLQueryResultColumn getColumnIfTrivialExpression() {
        return this.column;
    }

    void propagateColumnDefinition(@Nullable SQLQueryResultColumn resultColumn, @NotNull SQLQueryRecognitionContext statistics) {
        // TODO consider ambiguity
        if (resultColumn != null) {
            this.column = resultColumn;
            this.columnName.setDefinition(resultColumn.symbol.getDefinition());
        } else {
            this.columnName.getSymbol().setSymbolClass(SQLQuerySymbolClass.ERROR);
            statistics.appendError(this.columnName, "Column not found in dataset");
        }
    }

    @Override
    void propagateContext(@NotNull SQLQueryDataContext context, @NotNull SQLQueryRecognitionContext statistics) {
        SQLDialect dialect = context.getDialect();
        SQLQueryExprType type;
        if (this.tableName != null && this.tableName.isNotClassified() && this.columnName.isNotClassified()) {
            SourceResolutionResult rr = context.resolveSource(statistics.getMonitor(), this.tableName.toListOfStrings());
            if (rr != null) {
                this.tableName.setDefinition(rr);
                SQLQueryResultColumn resultColumn = rr.source.getDataContext().resolveColumn(statistics.getMonitor(), this.columnName.getName());
                this.propagateColumnDefinition(resultColumn, statistics);
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
            }

            if (forcedClass != null) {
                this.columnName.getSymbol().setSymbolClass(forcedClass);
                type = forcedClass == SQLQuerySymbolClass.STRING ? SQLQueryExprType.STRING : SQLQueryExprType.UNKNOWN;
            } else {
                this.propagateColumnDefinition(resultColumn, statistics);
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
        String name = this.tableName == null ? this.columnName.getName() 
                : this.tableName.toIdentifierString() + "." + this.columnName.getName();
        String type = this.type == null ? "<NULL>" : this.type.toString();
        return "ColumnReference[" + name + ":" + type + "]";
    }
}