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
package org.jkiss.dbeaver.model.sql.semantics.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

import java.util.List;

/**
 * Describes UPDATE statement
 */
public class SQLQueryTableUpdateModel extends SQLQueryModelContent {
    @Nullable
    private final SQLQueryRowsSourceModel targetRows;
    @Nullable
    private final List<SQLQueryTableUpdateSetClauseModel> setClauseList;
    @Nullable
    private final SQLQueryRowsSourceModel sourceRows;
    @Nullable
    private final SQLQueryValueExpression whereClause;
    @Nullable
    private final SQLQueryValueExpression orderByClause;
    @Nullable
    private SQLQueryDataContext givenContext = null;
    @Nullable
    private SQLQueryDataContext resultContext = null;
    
    public SQLQueryTableUpdateModel(
        @NotNull STMTreeNode syntaxNode,
        @Nullable SQLQueryRowsSourceModel targetRows,
        @Nullable List<SQLQueryTableUpdateSetClauseModel> setClauseList,
        @Nullable SQLQueryRowsSourceModel sourceRows,
        @Nullable SQLQueryValueExpression whereClause,
        @Nullable SQLQueryValueExpression orderByClause
    ) {
        super(syntaxNode.getRealInterval(), syntaxNode);
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
    public List<SQLQueryTableUpdateSetClauseModel> getSetClauseList() {
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
        this.givenContext = context;
        SQLQueryDataContext targetContext;
        if (this.targetRows != null) {
            targetContext = this.targetRows.propagateContext(context, statistics);
            
            if (this.setClauseList != null) {
                for (SQLQueryTableUpdateSetClauseModel updateSetClauseModel : this.setClauseList) {
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
            for (SQLQueryTableUpdateSetClauseModel setClauseModel : this.setClauseList) {
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
        
        this.resultContext = context;
    }

    @Override
    public SQLQueryDataContext getGivenDataContext() {
        return this.givenContext;
    }
    
    @Override
    public SQLQueryDataContext getResultDataContext() {
        return this.resultContext;
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
        return visitor.visitTableStatementUpdate(this, arg);
    }
}
