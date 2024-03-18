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
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLQuerySymbolEntry;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SQLQueryResultTupleContext.SQLQueryResultColumn;

import java.util.List;


public class SQLQueryTableInsertModel extends SQLQueryTableStatementModel {
    @Nullable
    private final List<SQLQuerySymbolEntry> columnNames;
    @Nullable
    private final SQLQueryRowsSourceModel valuesRows;
    
    public SQLQueryTableInsertModel(
        @NotNull Interval region,
        @Nullable SQLQueryRowsTableDataModel tableModel,
        @Nullable List<SQLQuerySymbolEntry> columnNames,
        @Nullable SQLQueryRowsSourceModel valuesRows
    ) {
        super(region, tableModel);
        this.columnNames = columnNames;
        this.valuesRows = valuesRows;
    }

    @Nullable
    public List<SQLQuerySymbolEntry> getColumnNames() {
        return this.columnNames;
    }

    @Nullable
    public SQLQueryRowsSourceModel getValuesRows() {
        return this.valuesRows;
    }

    @Override
    public void propagateContextImpl(@NotNull SQLQueryDataContext context, @NotNull SQLQueryRecognitionContext statistics) {
        if (this.columnNames != null) {
            for (SQLQuerySymbolEntry columnName : this.columnNames) {
                SQLQueryResultColumn column = context.resolveColumn(statistics.getMonitor(), columnName.getName());
                SQLQueryValueColumnReferenceExpression.propagateColumnDefinition(columnName, column, statistics);
            }
        }

        if (this.valuesRows != null) {
            SQLQueryDataContext valuesContext = this.valuesRows.propagateContext(context, statistics);
            // TODO validate column tuples consistency
        }
    }

    @Nullable
    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
        return visitor.visitTableStatementInsert(this, arg);
    }
}
