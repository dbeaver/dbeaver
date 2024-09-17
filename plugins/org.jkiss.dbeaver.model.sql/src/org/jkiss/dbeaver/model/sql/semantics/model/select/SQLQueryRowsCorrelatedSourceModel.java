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
package org.jkiss.dbeaver.model.sql.semantics.model.select;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.sql.semantics.*;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryResultColumn;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes a subquery source that have an alias and optionally columns list
 */
public class SQLQueryRowsCorrelatedSourceModel extends SQLQueryRowsSourceModel {
    @NotNull
    private final SQLQueryRowsSourceModel source;
    @NotNull
    private final SQLQuerySymbolEntry alias;
    @NotNull
    private final List<SQLQuerySymbolEntry> correlationColumNames;

    public SQLQueryRowsCorrelatedSourceModel(
        @NotNull STMTreeNode syntaxNode,
        @NotNull SQLQueryRowsSourceModel source,
        @NotNull SQLQuerySymbolEntry alias,
        @NotNull List<SQLQuerySymbolEntry> correlationColumNames
    ) {
        super(syntaxNode, source);
        this.source = source;
        this.alias = alias;
        this.correlationColumNames = correlationColumNames;
    }

    @NotNull
    public SQLQueryRowsSourceModel getSource() {
        return this.source;
    }

    @NotNull
    public SQLQuerySymbolEntry getAlias() {
        return this.alias;
    }

    @NotNull
    public List<SQLQuerySymbolEntry> getCorrelationColumNames() {
        return this.correlationColumNames;
    }

    @NotNull
    @Override
    protected SQLQueryDataContext propagateContextImpl(
        @NotNull SQLQueryDataContext context,
        @NotNull SQLQueryRecognitionContext statistics
    ) {
        context = this.source.propagateContext(context, statistics);
        
        if (this.alias.isNotClassified()) {
            context = context.extendWithTableAlias(this.alias.getSymbol(), this);
            
            this.alias.getSymbol().setDefinition(this.alias);
            if (this.alias.isNotClassified()) {
                this.alias.getSymbol().setSymbolClass(SQLQuerySymbolClass.TABLE_ALIAS);
            }

            context = prepareColumnsCorrelation(context, this.correlationColumNames, this);
        }
        return context;
    }

    /**
     * Associate correlated source column names symbols with its definition
     */
    @NotNull
    public static SQLQueryDataContext prepareColumnsCorrelation(
        @NotNull SQLQueryDataContext context,
        @NotNull List<SQLQuerySymbolEntry> correlationColumNames,
        @NotNull SQLQueryRowsSourceModel columnsSource
    ) {
        if (correlationColumNames.size() > 0) {
            List<SQLQueryResultColumn> columns = new ArrayList<>(context.getColumnsList());
            for (int i = 0; i < columns.size() && i < correlationColumNames.size(); i++) {
                SQLQuerySymbolEntry correlatedNameDef = correlationColumNames.get(i);
                if (correlatedNameDef.isNotClassified()) {
                    SQLQueryResultColumn oldColumn = columns.get(i);
                    SQLQuerySymbol correlatedName = correlatedNameDef.getSymbol();
                    correlatedName.setDefinition(correlatedNameDef);
                    correlatedName.setSymbolClass(SQLQuerySymbolClass.COLUMN_DERIVED);
                    correlatedNameDef.setDefinition(oldColumn.symbol.getDefinition());
                    columns.set(i, new SQLQueryResultColumn(i, correlatedName, columnsSource, null, null, oldColumn.type));
                }
            }
            context = context.overrideResultTuple(columnsSource, columns, context.getPseudoColumnsList());
        }
        return context;
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T node) {
        return visitor.visitRowsCorrelatedSource(this, node);
    }
}
