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
import org.jkiss.dbeaver.model.impl.struct.RelationalObjectType;
import org.jkiss.dbeaver.model.sql.semantics.*;
import org.jkiss.dbeaver.model.sql.semantics.context.*;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryMemberAccessEntry;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryTupleRefEntry;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsSourceModel;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

import java.util.Set;

/**
 * Describes several columns from the table
 */
public class SQLQueryValueTupleReferenceExpression extends SQLQueryValueExpression {
    @NotNull 
    private final SQLQueryComplexName tableName;

    @Nullable
    private final SQLQueryMemberAccessEntry memberAccessEntry;

    @Nullable
    private final SQLQueryTupleRefEntry tupleRefEntry;

    @Nullable
    private SQLQueryRowsSourceModel tupleSource = null;

    public SQLQueryValueTupleReferenceExpression(
        @NotNull STMTreeNode syntaxNode,
        @NotNull SQLQueryComplexName tableName,
        @Nullable SQLQueryMemberAccessEntry memberAccessEntry,
        @Nullable SQLQueryTupleRefEntry tupleRefEntry
    ) {
        super(syntaxNode);
        this.tableName = tableName;
        this.memberAccessEntry = memberAccessEntry;
        this.tupleRefEntry = tupleRefEntry;
    }

    @Nullable
    @Override
    public SQLQuerySymbolClass getAssociatedSymbolClass() {
        return SQLQuerySemanticUtils.getIdentifierSymbolClass(this.tableName);
    }

    @NotNull 
    public SQLQueryComplexName getTableName() {
        return this.tableName;
    }

    @Nullable
    public SQLQueryTupleRefEntry getTupleRefEntry() {
        return this.tupleRefEntry;
    }
    
    @Nullable
    public SQLQueryRowsSourceModel getTupleSource() {
        return this.tupleSource;
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
        SQLQueryExprType result;
        if (this.tableName.isNotClassified()) {
            SQLQuerySymbolOrigin tableNameOrigin = new SQLQuerySymbolOrigin.RowsDataRef(context);
            if (this.tableName.invalidPartsCount == 0) {
                SourceResolutionResult rr = context.getRowsSources().findReferencedSourceExact(this.tableName);
                if (rr != null) {
                    this.tupleSource = rr.source;
                    SQLQuerySemanticUtils.setNamePartsDefinition(this.tableName, rr, tableNameOrigin);
                    if (this.memberAccessEntry != null) {
                        this.memberAccessEntry.setOrigin(new SQLQuerySymbolOrigin.ColumnRefFromReferencedContext(rr));
                    }
                    if (this.tupleRefEntry != null) {
                        this.tupleRefEntry.setOrigin(
                            new SQLQuerySymbolOrigin.ExpandableRowsTupleRef(this.getSyntaxNode(), context, rr)
                        );
                    }
                    result = SQLQueryExprType.forReferencedRow(this.tableName, rr);
                } else {
                    this.tableName.parts.forEach(p -> p.getSymbol().setSymbolClass(SQLQuerySymbolClass.ERROR));
                    statistics.appendError(
                        this.tableName.syntaxNode,
                        "Table or subquery " + this.tableName.getNameString() + " not found"
                    );
                    result = SQLQueryExprType.UNKNOWN;
                }
            } else {
                SQLQuerySemanticUtils.performPartialResolution(
                    context.getRowsSources(),
                    statistics,
                    this.tableName,
                    tableNameOrigin,
                    SQLQuerySymbolOrigin.DbObjectFilterMode.VALUE,
                    SQLQuerySymbolClass.ERROR
                );
                statistics.appendError(this.getSyntaxNode(), "Invalid tuple reference");
                result = SQLQueryExprType.UNKNOWN;
            }
        } else {
            result = this.type;
        }

        return result;
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
        return visitor.visitValueTupleRefExpr(this, arg);
    }
    
    @Override
    public String toString() {
        String name = this.tableName.getNameString();
        String type = this.type == null ? "<NULL>" : this.type.toString();
        return "TupleReference[" + name + ":" + type + "]";
    }
}