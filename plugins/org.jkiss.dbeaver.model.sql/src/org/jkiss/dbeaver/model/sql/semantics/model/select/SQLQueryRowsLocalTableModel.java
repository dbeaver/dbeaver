/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.sql.semantics.model.select;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbol;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolClass;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolEntry;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryResultColumn;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsSourceContext;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.sql.semantics.model.ddl.SQLQueryColumnSpec;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

import java.util.ArrayList;
import java.util.List;

/** Row source defined by a table-valued variable or a temporary table within a procedural statement. */
public class SQLQueryRowsLocalTableModel extends SQLQueryRowsSourceModel {
    @NotNull
    private final SQLQuerySymbolEntry name;
    @NotNull
    private final List<SQLQueryColumnSpec> columns;

    public SQLQueryRowsLocalTableModel(
        @NotNull STMTreeNode node, @NotNull SQLQuerySymbolEntry name, @NotNull List<SQLQueryColumnSpec> columns
    ) {
        super(node, columns.toArray(SQLQueryColumnSpec[]::new));
        this.name = name;
        this.columns = List.copyOf(columns);
    }

    @NotNull
    public SQLQuerySymbolEntry getName() {
        return this.name;
    }

    @Override
    @NotNull
    protected SQLQueryRowsSourceContext resolveRowSourcesImpl(
        @NotNull SQLQueryRowsSourceContext context,
        @NotNull SQLQueryRecognitionContext statistics
    ) {
        return context.reset();
    }

    @Override
    @NotNull
    protected SQLQueryRowsDataContext resolveRowDataImpl(
        @NotNull SQLQueryRowsDataContext context,
        @NotNull SQLQueryRecognitionContext statistics
    ) {
        List<SQLQueryResultColumn> result = new ArrayList<>(this.columns.size());
        for (SQLQueryColumnSpec spec : this.columns) {
            SQLQuerySymbolEntry entry = spec.getColumnName();
            SQLQuerySymbol symbol = entry == null ? new SQLQuerySymbol("?") : entry.getSymbol();
            if (entry != null && entry.isNotClassified()) {
                symbol.setSymbolClass(SQLQuerySymbolClass.COLUMN);
            }
            result.add(new SQLQueryResultColumn(result.size(), symbol, this, null, null, spec.getDeclaredColumnType()));
        }
        return this.getRowsSources().makeTuple(this, result, List.of());
    }

    @Override
    @Nullable
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @Nullable T arg) {
        return visitor.visitLocalTable(this, arg);
    }
}
