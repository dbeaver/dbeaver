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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Describes Common Table Expression (CTE)
 */
public class SQLQueryRowsCteModel extends SQLQueryRowsSourceModel {

    private final boolean isRecursive;
    @NotNull
    private final List<SQLQueryRowsCteSubqueryModel> subqueries;
    @NotNull
    private final SQLQueryRowsSourceModel resultQuery;

    public SQLQueryRowsCteModel(
        @NotNull STMTreeNode syntaxNode,
        boolean isRecursive,
        @NotNull List<SQLQueryRowsCteSubqueryModel> subqueries,
        @NotNull SQLQueryRowsSourceModel resultQuery
    ) {
        super(syntaxNode, resultQuery);
        this.isRecursive = isRecursive;
        this.resultQuery = resultQuery;

        this.subqueries = List.copyOf(subqueries);
        this.subqueries.forEach(super::registerSubnode);
    }

    /**
     * Get all subqueries of the CTE and CTE query itself
     */
    @NotNull
    public List<SQLQueryRowsSourceModel> getAllQueries() {
        List<SQLQueryRowsSourceModel> queries = new ArrayList<>(this.subqueries.size() + 1);
        queries.addAll(this.subqueries);
        queries.add(this.resultQuery);
        return queries;
    }

    @NotNull
    @Override
    protected SQLQueryDataContext propagateContextImpl(
        @NotNull SQLQueryDataContext context,
        @NotNull SQLQueryRecognitionContext statistics
    ) {
        SQLQueryDataContext aggregatedContext = context;
        
        if (this.isRecursive) {
            //https://stackoverflow.com/questions/35248217/how-to-use-multiple-ctes-in-a-single-sql-query
            
            // TODO consider subqueries topological sorting according to their interdependencies, also consider recursive dependency 
            
            for (SQLQueryRowsCteSubqueryModel subquery : this.subqueries) { // bind all the query names at first
                subquery.propagateContext(context, statistics);
                if (subquery.subqueryName != null) {
                    aggregatedContext = aggregatedContext.combine(
                        aggregatedContext.hideSources().extendWithTableAlias(subquery.subqueryName.getSymbol(), subquery)
                    );
                } else {
                    // should never happen according to the grammar
                    aggregatedContext = aggregatedContext.markHasUnresolvedSource();
                }
            }
            
            for (SQLQueryRowsCteSubqueryModel subquery : this.subqueries) { // then resolve subqueries themselves
                // TODO but column names are not backwards-visible still
                if (subquery.source != null) {
                    context = subquery.source.propagateContext(aggregatedContext, statistics).hideSources();
                    subquery.propagateContext(context, statistics);
                    subquery.prepareAliasDefinition();
                }
            }
        } else {
            for (SQLQueryRowsCteSubqueryModel subquery : this.subqueries) {
                SQLQueryDataContext subqueryResult = (
                    subquery.source == null
                        ? aggregatedContext.overrideResultTuple(null, Collections.emptyList(), Collections.emptyList())
                        : subquery.source.propagateContext(aggregatedContext, statistics)
                ).hideSources();
                SQLQueryDataContext currCtx = subquery.subqueryName == null
                    ? subqueryResult
                    : subqueryResult.extendWithTableAlias(subquery.subqueryName.getSymbol(), subquery);

                subquery.prepareAliasDefinition();

                // TODO error on mismatch number of columns and hide unmapped columns
                currCtx = SQLQueryRowsCorrelatedSourceModel.prepareColumnsCorrelation(currCtx, subquery.columNames, subquery);
                subquery.propagateContext(currCtx, statistics);
                aggregatedContext = aggregatedContext.combine(currCtx);
            }
        }
        
        return this.resultQuery.propagateContext(aggregatedContext, statistics).hideSources();
    }


    @Nullable
    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
        return visitor.visitRowsCte(this, arg);
    }
}

