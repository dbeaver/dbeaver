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
import org.jkiss.dbeaver.ui.editors.sql.semantics.*;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SourceResolutionResult;

public class SQLQueryValueTupleReferenceExpression extends SQLQueryValueExpression {
    @NotNull 
    private final SQLQueryQualifiedName tableName;
    
    private SQLQueryRowsSourceModel tupleSource = null;
    
    public SQLQueryValueTupleReferenceExpression(
        @NotNull Interval range,
        @NotNull SQLQueryQualifiedName tableName
    ) {
        super(range);
        this.tableName = tableName;
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
    void propagateContext(@NotNull SQLQueryDataContext context, @NotNull SQLQueryRecognitionContext statistics) {
        if (this.tableName.isNotClassified()) {
            SourceResolutionResult rr = context.resolveSource(statistics.getMonitor(), this.tableName.toListOfStrings());
            if (rr != null) {
                this.tupleSource = rr.source;
                this.tableName.setDefinition(rr);
            } else {
                this.tableName.setSymbolClass(SQLQuerySymbolClass.ERROR);
                statistics.appendError(this.tableName.entityName, "Table or subquery not found");
            }
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