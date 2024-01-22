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
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLQuerySymbol;
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLQuerySymbolClass;
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLQuerySymbolEntry;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SQLQueryResultTupleContext.SQLQueryResultColumn;

import java.util.ArrayList;
import java.util.List;

public class SQLQueryRowsCorrelatedSourceModel extends SQLQueryRowsSourceModel {
    private final SQLQueryRowsSourceModel source;
    private final SQLQuerySymbolEntry alias;
    private final List<SQLQuerySymbolEntry> correlationColumNames;
    private final boolean forSubquery;

    public SQLQueryRowsCorrelatedSourceModel(
        @NotNull Interval range,
        @NotNull SQLQueryRowsSourceModel source,
        @NotNull SQLQuerySymbolEntry alias,
        @NotNull List<SQLQuerySymbolEntry> correlationColumNames,
        boolean forSubquery
    ) {
        super(range);
        this.source = source;
        this.alias = alias;
        this.correlationColumNames = correlationColumNames;
        this.forSubquery = forSubquery;
    }

    public SQLQueryRowsSourceModel getSource() {
        return this.source;
    }

    public SQLQuerySymbolEntry getAlias() {
        return this.alias;
    }

    public List<SQLQuerySymbolEntry> getCorrelationColumNames() {
        return this.correlationColumNames;
    }

    @NotNull
    @Override
    protected SQLQueryDataContext propagateContextImpl(
        @NotNull SQLQueryDataContext context,
        @NotNull SQLQueryRecognitionContext statistics
    ) {
        if (this.alias.isNotClassified()) {
            context = this.source.propagateContext(context, statistics).extendWithTableAlias(this.alias.getSymbol(), this);
            this.alias.getSymbol().setDefinition(this.alias);
            if (this.alias.isNotClassified()) {
                this.alias.getSymbol().setSymbolClass(SQLQuerySymbolClass.TABLE_ALIAS);
            }

            if (correlationColumNames.size() > 0) {
                List<SQLQueryResultColumn> columns = new ArrayList<>(context.getColumnsList());
                for (int i = 0; i < columns.size() && i < correlationColumNames.size(); i++) {
                    SQLQuerySymbolEntry correlatedNameDef = correlationColumNames.get(i);
                    if (correlatedNameDef.isNotClassified()) {
                        SQLQuerySymbol correlatedName = correlatedNameDef.getSymbol();
                        correlatedName.setDefinition(correlatedNameDef);
                        correlatedName.setSymbolClass(SQLQuerySymbolClass.COLUMN_DERIVED);
                        correlatedNameDef.setDefinition(columns.get(i).symbol.getDefinition());
                        columns.set(i, new SQLQueryResultColumn(correlatedName, this, null, null));
                    }
                }
                context = context.overrideResultTuple(columns);
            }
        }
        return context;
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T node) {
        return visitor.visitRowsCorrelatedSource(this, node);
    }
}