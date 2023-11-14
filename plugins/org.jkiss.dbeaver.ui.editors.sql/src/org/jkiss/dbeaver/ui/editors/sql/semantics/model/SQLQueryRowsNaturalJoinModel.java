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
import org.jkiss.dbeaver.ui.editors.sql.semantics.*;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SQLQueryDataContext;

import java.util.List;

public class SQLQueryRowsNaturalJoinModel extends SQLQueryRowsSetOperationModel {
    private final SQLQueryValueExpression condition;
    private final List<SQLQuerySymbolEntry> columsToJoin;
    
    public SQLQueryRowsNaturalJoinModel(
        @NotNull SQLQueryRowsSourceModel left,
        @NotNull SQLQueryRowsSourceModel right,
        @Nullable SQLQueryValueExpression condition
    ) {
        super(left, right);
        this.condition = condition;
        this.columsToJoin = null;
    }
    
    public SQLQueryRowsNaturalJoinModel(
        @NotNull SQLQueryRowsSourceModel left,
        @NotNull SQLQueryRowsSourceModel right,
        @Nullable List<SQLQuerySymbolEntry> columsToJoin
    ) {
        super(left, right);
        this.condition = null;
        this.columsToJoin = columsToJoin;
    }

    @NotNull
    @Override
    protected SQLQueryDataContext propagateContextImpl(@NotNull SQLQueryDataContext context, @NotNull SQLQueryRecognitionContext statistics) {
        SQLQueryDataContext left = this.left.propagateContext(context, statistics);
        SQLQueryDataContext right = this.right.propagateContext(context, statistics);
        if (this.columsToJoin != null) {
            for (SQLQuerySymbolEntry column : columsToJoin) {
                SQLQuerySymbol symbol = column.getSymbol();
                SQLQuerySymbolDefinition leftColumnDef = left.resolveColumn(column.getName());
                SQLQuerySymbolDefinition rightColumnDef = right.resolveColumn(column.getName());
                if (leftColumnDef != null && rightColumnDef != null) {
                    symbol.setSymbolClass(SQLQuerySymbolClass.COLUMN); 
                    symbol.setDefinition(column); // TODO multiple definitions per symbol
                } else {
                    if (leftColumnDef != null) {
                        statistics.appendError(column, "Column not found to the left of join");
                    } else {
                        statistics.appendError(column, "Column not found to the right of join");
                    }
                    symbol.setSymbolClass(SQLQuerySymbolClass.ERROR);
                }
            }
        }
        
        SQLQueryDataContext combinedContext = left.combine(right);
        if (this.condition != null) {
            this.condition.propagateContext(combinedContext, statistics);
        }
        return combinedContext;
    }
}