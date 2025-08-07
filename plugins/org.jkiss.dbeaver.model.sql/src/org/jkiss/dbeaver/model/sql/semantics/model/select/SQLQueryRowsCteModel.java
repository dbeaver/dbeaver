/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsSourceContext;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.stm.STMTreeNode;
import org.jkiss.utils.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes Common Table Expression (CTE)
 */
public class SQLQueryRowsCteModel extends SQLQueryRowsSourceModel {

    @NotNull
    private final List<SQLQueryRowsCteSubqueryModel> subqueries;
    @NotNull
    private final SQLQueryRowsSourceModel resultQuery;

    public SQLQueryRowsCteModel(
        @NotNull STMTreeNode syntaxNode,
        @NotNull List<SQLQueryRowsCteSubqueryModel> subqueries,
        @NotNull SQLQueryRowsSourceModel resultQuery
    ) {
        super(syntaxNode, resultQuery);
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

    @Override
    protected SQLQueryRowsSourceContext resolveRowSourcesImpl(
        @NotNull SQLQueryRowsSourceContext context,
        @NotNull SQLQueryRecognitionContext statistics
    ) {
        SQLQueryRowsSourceContext cteContext = context.appendCteSources(this.subqueries.stream().map(
            q -> Pair.of(q.subqueryName, (SQLQueryRowsSourceModel) q)
        ).toList());
        this.subqueries.forEach(q -> q.resolveRowSources(cteContext, statistics));
        this.resultQuery.resolveRowSources(cteContext, statistics);
        return context.reset();
    }

    @Override
    protected SQLQueryRowsDataContext resolveRowDataImpl(
        @NotNull SQLQueryRowsDataContext context,
        @NotNull SQLQueryRecognitionContext statistics
    ) {
        return this.resultQuery.getRowsDataContext();
    }

    @Nullable
    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
        return visitor.visitRowsCte(this, arg);
    }
}

