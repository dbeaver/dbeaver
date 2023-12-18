/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLQueryQualifiedName;
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLQuerySymbol;
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLQuerySymbolClass;
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLQuerySymbolDefinition;
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLQuerySymbolEntry;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SourceResolutionResult;

public class SQLQueryValueColumnReferenceExpression extends SQLQueryValueExpression {
    private final SQLQueryQualifiedName tableName;
    private final SQLQuerySymbolEntry columnName;

    public SQLQueryValueColumnReferenceExpression(@NotNull SQLQuerySymbolEntry columnName) {
        this.tableName = null;
        this.columnName = columnName;
    }

    public SQLQueryValueColumnReferenceExpression(@NotNull SQLQueryQualifiedName tableName, @NotNull SQLQuerySymbolEntry columnName) {
        this.tableName = tableName;
        this.columnName = columnName;
    }

    @NotNull
    @Override
    public SQLQuerySymbol getColumnNameIfTrivialExpression() {
        return this.columnName.getSymbol();
    }
    
    void propagateColumnDefinition(@Nullable SQLQuerySymbolDefinition columnDef, @NotNull SQLQueryRecognitionContext statistics) {
        // TODO consider ambiguity
        if (columnDef != null) {
            this.columnName.setDefinition(columnDef);
        } else {
            this.columnName.getSymbol().setSymbolClass(SQLQuerySymbolClass.ERROR);
            statistics.appendError(this.columnName, "Column not found in dataset");
        }
    }
    
    @Override
    void propagateContext(@NotNull SQLQueryDataContext context, @NotNull SQLQueryRecognitionContext statistics) {
        SQLDialect dialect = context.getDialect();
        if (this.tableName != null && this.tableName.isNotClassified() && this.columnName.isNotClassified()) {
            SourceResolutionResult rr = context.resolveSource(this.tableName.toListOfStrings());
            if (rr != null) {
                this.tableName.setDefinition(rr);
                SQLQuerySymbolDefinition columnDef = rr.source.getDataContext().resolveColumn(this.columnName.getName());
                this.propagateColumnDefinition(columnDef, statistics);
            } else {
                this.tableName.setSymbolClass(SQLQuerySymbolClass.ERROR);
                statistics.appendError(this.tableName.entityName, "Table or subquery not found");
            }
        } else if (this.tableName == null && this.columnName.isNotClassified()) {
            SQLQuerySymbolDefinition columnDef = context.resolveColumn(this.columnName.getName());
            
            SQLQuerySymbolClass forcedClass = null;
            if (columnDef == null) {
                String rawString = columnName.getRawName();
                if (dialect.isQuotedString(rawString)) {
                    forcedClass = SQLQuerySymbolClass.STRING;
                } else {
                    boolean isQuotedIdentifier = dialect.isQuotedIdentifier(this.columnName.getRawName());
                    char quoteChar = this.columnName.getRawName().charAt(0);
                    if ((!isQuotedIdentifier && (quoteChar == '"' || quoteChar == '`' || quoteChar == '\''))
                        || (isQuotedIdentifier && columnDef == null)) {
                        forcedClass = switch (quoteChar) {
                            case '\'' -> SQLQuerySymbolClass.STRING;
                            case '"', '`' -> SQLQuerySymbolClass.QUOTED;
                            default -> null;
                        };
                    }
                }
            }
            
            if (forcedClass != null) {
                this.columnName.getSymbol().setSymbolClass(forcedClass);
            } else {
                this.propagateColumnDefinition(columnDef, statistics);
            }
        }
    }
}