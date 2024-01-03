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

public class SQLQueryRowsSelectionFilterModel extends SQLQueryRowsSourceModel { // see tableExpression
    private final SQLQueryRowsSourceModel fromSource;
    private final SQLQueryValueExpression whereClause;
    private final SQLQueryValueExpression havingClause;
    private final SQLQueryValueExpression groupByClause;
    private final SQLQueryValueExpression orderByClause;

    public SQLQueryRowsSelectionFilterModel(
        @NotNull SQLQueryRowsSourceModel fromSource,
        @Nullable SQLQueryValueExpression whereClause,
        @Nullable SQLQueryValueExpression havingClause,
        @Nullable SQLQueryValueExpression groupByClause,
        @Nullable SQLQueryValueExpression orderByClause
    ) {
        this.fromSource = fromSource;
        this.whereClause = whereClause;
        this.havingClause = havingClause;
        this.groupByClause = groupByClause;
        this.orderByClause = orderByClause;
    }

    @NotNull
    @Override
    protected SQLQueryDataContext propagateContextImpl(@NotNull SQLQueryDataContext context, @NotNull SQLQueryRecognitionContext statistics) {
        SQLQueryDataContext result = fromSource.propagateContext(context, statistics);
        
        if (this.whereClause != null) {
            this.whereClause.propagateContext(result, statistics);
        }
        if (this.havingClause != null) {
            this.havingClause.propagateContext(result, statistics);
        }
        if (this.groupByClause != null) {
            this.groupByClause.propagateContext(result, statistics);
        }
        if (this.orderByClause != null) {
            this.orderByClause.propagateContext(result, statistics);
        }
             
        return result;
    }
}