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
package org.jkiss.dbeaver.model.sql.semantics.model.expressions;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryQualifiedName;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolClass;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolOrigin;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryExprType;
import org.jkiss.dbeaver.model.sql.semantics.context.SourceResolutionResult;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryMemberAccessEntry;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsSourceModel;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

/**
 * Describes several columns from the table
 */
public class SQLQueryValueTupleReferenceExpression extends SQLQueryValueExpression {
    @NotNull 
    private final SQLQueryQualifiedName tableName;

    @Nullable
    private final SQLQueryMemberAccessEntry memberAccessEntry;

    @Nullable
    private SQLQueryRowsSourceModel tupleSource = null;

    public SQLQueryValueTupleReferenceExpression(
        @NotNull STMTreeNode syntaxNode,
        @NotNull SQLQueryQualifiedName tableName,
        @Nullable SQLQueryMemberAccessEntry memberAccessEntry
    ) {
        super(syntaxNode);
        this.tableName = tableName;
        this.memberAccessEntry = memberAccessEntry;
    }

    @NotNull 
    public SQLQueryQualifiedName getTableName() {
        return this.tableName;
    }
    
    @Nullable
    public SQLQueryRowsSourceModel getTupleSource() {
        return this.tupleSource;
    }
    
    @Override
    protected void propagateContextImpl(@NotNull SQLQueryDataContext context, @NotNull SQLQueryRecognitionContext statistics) {
        if (this.tableName.isNotClassified()) {
            SQLQuerySymbolOrigin tableNameOrigin = new SQLQuerySymbolOrigin.ValueRefFromContext(context);
            if (this.tableName.invalidPartsCount == 0) {
                SourceResolutionResult rr = context.resolveSource(statistics.getMonitor(), this.tableName.toListOfStrings());
                if (rr != null) {
                    this.tupleSource = rr.source;
                    this.tableName.setDefinition(rr, tableNameOrigin);
                    if (this.memberAccessEntry != null) {
                        this.memberAccessEntry.setOrigin(new SQLQuerySymbolOrigin.ColumnRefFromReferencedContext(rr));
                    }
                } else {
                    this.tableName.setSymbolClass(SQLQuerySymbolClass.ERROR);
                    statistics.appendError(this.tableName.entityName,
                        "Table or subquery " + this.tableName.toIdentifierString() + " not found");
                }
            } else {
                SQLQueryQualifiedName.performPartialResolution(context, statistics, this.tableName, tableNameOrigin);
                statistics.appendError(this.getSyntaxNode(), "Invalid tuple reference");
            }
            type = SQLQueryExprType.UNKNOWN;
        }
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
        return visitor.visitValueTupleRefExpr(this, arg);
    }
    
    @Override
    public String toString() {
        String name = this.tableName.toIdentifierString();
        String type = this.type == null ? "<NULL>" : this.type.toString();
        return "TupleReference[" + name + ":" + type + "]";
    }
}