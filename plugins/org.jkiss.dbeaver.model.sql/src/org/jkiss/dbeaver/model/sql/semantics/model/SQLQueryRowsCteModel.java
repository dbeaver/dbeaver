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


import org.antlr.v4.runtime.misc.Interval;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolClass;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolEntry;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;

import java.util.ArrayList;
import java.util.List;


public class SQLQueryRowsCteModel extends SQLQueryRowsSourceModel {

    private final boolean isRecursive;
    private final List<SQLQueryRowsCteSubqueryModel> subqueries = new ArrayList<>();
    private final SQLQueryRowsSourceModel resultQuery;

    public static class SQLQueryRowsCteSubqueryModel extends SQLQueryRowsSourceModel {
        public final SQLQuerySymbolEntry subqueryName;
        public final List<SQLQuerySymbolEntry> columNames;
        public final SQLQueryRowsSourceModel source;

        public SQLQueryRowsCteSubqueryModel(
            @NotNull Interval region,
            @NotNull SQLQuerySymbolEntry subqueryName,
            @NotNull List<SQLQuerySymbolEntry> columNames,
            @NotNull SQLQueryRowsSourceModel source
        ) {
            super(region);
            this.subqueryName = subqueryName;
            this.columNames = columNames;
            this.source = source;
        }
        
        public void prepareAliasDefinition() {
            this.subqueryName.getSymbol().setDefinition(this.subqueryName);
            if (this.subqueryName.isNotClassified()) {
                this.subqueryName.getSymbol().setSymbolClass(SQLQuerySymbolClass.TABLE_ALIAS);
            }
        }
        
        @NotNull
        @Override
        protected SQLQueryDataContext propagateContextImpl(
            @NotNull SQLQueryDataContext context,
            @NotNull SQLQueryRecognitionContext statistics
        ) {
            return context; // just apply given context 
        }

        @Nullable
        @Override
        protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T node) {
            return visitor.visitRowsCteSubquery(this, node);
        }
    }

    public SQLQueryRowsCteModel(@NotNull Interval region, boolean isRecursive, @NotNull SQLQueryRowsSourceModel resultQuery) {
        super(region, resultQuery);
        this.isRecursive = isRecursive;
        this.resultQuery = resultQuery;
    }
    
    public void addSubquery(
        @NotNull Interval range,
        @NotNull SQLQuerySymbolEntry subqueryName,
        @NotNull List<SQLQuerySymbolEntry> columnNames,
        @NotNull SQLQueryRowsSourceModel source
    ) {
        SQLQueryRowsCteSubqueryModel subquery = new SQLQueryRowsCteSubqueryModel(range, subqueryName, columnNames, source);
        this.subqueries.add(subquery);
        super.registerSubnode(subquery);
    }

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
                aggregatedContext = aggregatedContext.combine(
                    aggregatedContext.hideSources().extendWithTableAlias(subquery.subqueryName.getSymbol(), subquery)
                );
            }
            
            for (SQLQueryRowsCteSubqueryModel subquery : this.subqueries) { // then resolve subqueries themselves
                if (subquery.subqueryName.isNotClassified()) {                    
                    // TODO but column names are not backwards-visible still 
                    context = subquery.source.propagateContext(aggregatedContext, statistics).hideSources();
                    subquery.propagateContext(context, statistics);
                    subquery.prepareAliasDefinition();
                }
            }
        } else {
            for (SQLQueryRowsCteSubqueryModel subquery : this.subqueries) {
                if (subquery.subqueryName.isNotClassified()) {
                    SQLQueryDataContext currCtx = subquery.source.propagateContext(aggregatedContext, statistics)
                        .hideSources()
                        .extendWithTableAlias(subquery.subqueryName.getSymbol(), subquery);
                    
                    subquery.prepareAliasDefinition();
                    
                    // TODO error on mismatch number of columns and hide unmapped columns
                    currCtx = SQLQueryRowsCorrelatedSourceModel.prepareColumnsCorrelation(currCtx, subquery.columNames, subquery);
                    subquery.propagateContext(currCtx, statistics);
                    aggregatedContext = aggregatedContext.combine(currCtx);
                }
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

