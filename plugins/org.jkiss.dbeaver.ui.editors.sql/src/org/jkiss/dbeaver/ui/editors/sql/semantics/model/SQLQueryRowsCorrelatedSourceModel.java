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
import org.jkiss.dbeaver.ui.editors.sql.semantics.*;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SQLQueryDataContext;

import java.util.ArrayList;
import java.util.List;

public class SQLQueryRowsCorrelatedSourceModel extends SQLQueryRowsSourceModel {
    private final SQLQueryRowsSourceModel source;
    private final SQLQuerySymbolEntry alias;
    private final List<SQLQuerySymbolEntry> correlationColumNames;
    
    public SQLQueryRowsCorrelatedSourceModel(
        @NotNull SQLQueryRowsSourceModel source,
        @NotNull SQLQuerySymbolEntry alias,
        @NotNull List<SQLQuerySymbolEntry> correlationColumNames
    ) {
        this.source = source;
        this.alias = alias;
        this.correlationColumNames = correlationColumNames;
    }

    @NotNull
    @Override
    protected SQLQueryDataContext propagateContextImpl(
        @NotNull SQLQueryDataContext context,
        @NotNull SQLQueryRecognitionContext statistics
    ) {
        if (this.alias.isNotClassified()) {
            context = source.propagateContext(context, statistics).extendWithTableAlias(this.alias.getSymbol(), source);
            this.alias.getSymbol().setDefinition(this.alias);
            if (this.alias.isNotClassified()) {
                this.alias.getSymbol().setSymbolClass(SQLQuerySymbolClass.TABLE_ALIAS);
            }
    
            if (correlationColumNames.size() > 0) {
                List<SQLQuerySymbol> columns = new ArrayList<>(context.getColumnsList());
                for (int i = 0; i < columns.size() && i < correlationColumNames.size(); i++) {
                    SQLQuerySymbolEntry correlatedNameDef = correlationColumNames.get(i);
                    if (correlatedNameDef.isNotClassified()) {
                        SQLQuerySymbol correlatedName = correlatedNameDef.getSymbol();
                        correlatedNameDef.setDefinition(columns.get(i).getDefinition());
                        correlatedName.setDefinition(correlatedNameDef);
                        columns.set(i, correlatedName);
                    }
                }
                context = context.overrideResultTuple(columns);
            }
        }
        return context;
    }
}