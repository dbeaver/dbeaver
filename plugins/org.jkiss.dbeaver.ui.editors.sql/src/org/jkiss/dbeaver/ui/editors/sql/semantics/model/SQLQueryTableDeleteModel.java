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

import java.util.Collections;


public class SQLQueryTableDeleteModel extends SQLQueryTableStatementModel {

    @Nullable
    private final SQLQueryValueExpression whereClause;
    @Nullable 
    private final SQLQueryRowsCorrelatedSourceModel aliasedTableModel;
    
    public SQLQueryTableDeleteModel(
        @NotNull Interval region,
        @Nullable SQLQueryRowsTableDataModel tableModel,
        @Nullable SQLQuerySymbolEntry alias, 
        @Nullable SQLQueryValueExpression whereClause
    ) {
        super(region, tableModel);
        this.whereClause = whereClause;
        
        if (alias != null && tableModel != null) {
            Interval correlatedRegion = Interval.of(tableModel.getInterval().a, alias.getInterval().b);
            this.aliasedTableModel = new SQLQueryRowsCorrelatedSourceModel(correlatedRegion, tableModel, alias, Collections.emptyList());
        } else {
            this.aliasedTableModel  = null;
        }
    }

    @Nullable
    public SQLQueryValueExpression getCondition() {
        return this.whereClause;
    }
    
    @Nullable
    public SQLQueryRowsCorrelatedSourceModel getAliasedTableModel() {
        return this.aliasedTableModel;
    }
    
    @Override
    public void propagateContextImpl(@NotNull SQLQueryDataContext context, @NotNull SQLQueryRecognitionContext statistics) {
        if (this.aliasedTableModel != null) {
            context = this.aliasedTableModel.propagateContext(context, statistics);
        }
        if (this.whereClause != null) {
            this.whereClause.propagateContext(context, statistics);
        }
    }

    @Nullable
    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
        return visitor.visitTableStatementDelete(this, arg);
    }
}
