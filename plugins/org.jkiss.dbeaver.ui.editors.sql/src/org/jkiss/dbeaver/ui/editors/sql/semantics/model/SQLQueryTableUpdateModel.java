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
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SQLQueryDataContext;

import java.util.List;


public class SQLQueryTableUpdateModel extends SQLQueryModelContent {
    @Nullable
    private final SQLQueryRowsSourceModel targetRows;
    @Nullable
    private final List<SetClauseModel> setClauseList;
    @Nullable
    private final SQLQueryRowsSourceModel sourceRows;
    @Nullable
    private final SQLQueryValueExpression whereClause;
    @Nullable
    private final SQLQueryValueExpression orderByClause;
    
    public SQLQueryTableUpdateModel(
        @NotNull Interval region,
        @Nullable SQLQueryRowsSourceModel targetRows,
        @Nullable List<SetClauseModel> setClauseList,
        @Nullable SQLQueryRowsSourceModel sourceRows,
        @Nullable SQLQueryValueExpression whereClause,
        @Nullable SQLQueryValueExpression orderByClause
    ) {
        super(region);
        this.targetRows = targetRows;
        this.setClauseList = setClauseList;
        this.sourceRows = sourceRows;
        this.whereClause = whereClause;
        this.orderByClause = orderByClause;
    }

    @Nullable
    public SQLQueryRowsSourceModel getTargetRows() {
        return targetRows;
    }

    @Nullable
    public List<SetClauseModel> getSetClauseList() {
        return setClauseList;
    }

    @Nullable
    public SQLQueryRowsSourceModel getSourceRows() {
        return sourceRows;
    }

    @Nullable
    public SQLQueryValueExpression getWhereClause() {
        return whereClause;
    }

    @Nullable
    public SQLQueryValueExpression getOrderByClause() {
        return orderByClause;
    }

    @Override
    void applyContext(@NotNull SQLQueryDataContext context, @NotNull SQLQueryRecognitionContext statistics) {
        SQLQueryDataContext targetContext;
        if (this.targetRows != null) {
            targetContext = this.targetRows.propagateContext(context, statistics);
            
            if (this.setClauseList != null) {
                for (SetClauseModel updateSetClauseModel : this.setClauseList) {
                    // resolve target columns against target set
                    for (SQLQueryValueExpression valueExpression : updateSetClauseModel.targets) {
                        valueExpression.propagateContext(targetContext, statistics);
                    }
                }
            }
        } else {
            // leave target column names as unresolved
            targetContext = context;
        }
        
        SQLQueryDataContext sourceContext = this.sourceRows != null ? this.sourceRows.propagateContext(context, statistics) : context;
        
        if (targetContext != context || sourceContext != context) {
            context = targetContext.combine(sourceContext);
        }
        
        if (this.setClauseList != null) {
            for (SetClauseModel setClauseModel : this.setClauseList) {
                // resolve source value expressions against combined participating sets
                for (SQLQueryValueExpression valueExpression : setClauseModel.sources) {
                    valueExpression.propagateContext(targetContext, statistics);
                }
            }
        }
        
        // TODO validate setClauseList
        
        if (this.whereClause != null) {
            this.whereClause.propagateContext(context, statistics);
        }
        if (this.orderByClause != null) {
            this.orderByClause.propagateContext(context, statistics);
        }
    }
    
    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
        return visitor.visitTableStatementUpdate(this, arg);
    }

    public static class SetClauseModel extends SQLQueryNodeModel {
        @NotNull
        public final List<SQLQueryValueExpression> targets;
        @NotNull
        public final List<SQLQueryValueExpression> sources;
        @NotNull
        public final String contents;

        public SetClauseModel(
            @NotNull Interval region,
            @NotNull List<SQLQueryValueExpression> targets,
            @NotNull List<SQLQueryValueExpression> sources,
            @NotNull String contents
        ) {
            super(region);
            this.targets = targets;
            this.sources = sources;
            this.contents = contents;
        }

        @Override
        protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
            return visitor.visitTableStatementUpdateSetClause(this, arg);
        }
    }
}
