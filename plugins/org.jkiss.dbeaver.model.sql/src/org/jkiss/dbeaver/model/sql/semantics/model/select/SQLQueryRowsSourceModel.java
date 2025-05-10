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

import org.antlr.v4.runtime.misc.Interval;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsSourceContext;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryModelContent;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModel;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

/**
 * Describes the semantics of a query part responsible for a data rows source representation (table, join, table-value, etc.)
 */
public abstract class SQLQueryRowsSourceModel extends SQLQueryModelContent {
    @Nullable
    private SQLQueryDataContext givenDataContext = null;
    @Nullable
    private SQLQueryDataContext resultDataContext = null;
    @Nullable
    private SQLQueryRowsSourceContext rowsSourceContext = null;
    @Nullable
    private SQLQueryRowsDataContext rowsDataContext = null;

    public SQLQueryRowsSourceModel(@NotNull STMTreeNode syntaxNode, @Nullable SQLQueryNodeModel... subnodes) {
        super(syntaxNode.getRealInterval(), syntaxNode, subnodes);
    }

    public SQLQueryRowsSourceModel(@NotNull Interval region, @NotNull STMTreeNode syntaxNode, @Nullable SQLQueryNodeModel ... subnodes) {
        super(region, syntaxNode, subnodes);
    }

    /**
     * Returns given data context before the semantics of this model item was applied
     */
    @Nullable
    @Override
    public SQLQueryDataContext getGivenDataContext() {
        return this.givenDataContext;
    }

    /**
     * Returns result data context, if it has been resolved. Otherwise, throws IllegalStateException.
     */
    @NotNull
    public SQLQueryDataContext getResultDataContext() {
        if (this.resultDataContext == null) {
            throw new IllegalStateException("Data context was not resolved for the rows source yet");
        } else {
            return this.resultDataContext;
        }
    }

    /**
     * Returns rows data context, if it has been resolved. Otherwise, throws IllegalStateException.
     */
    @NotNull
    public SQLQueryRowsDataContext getRowsDataContext() {
        if (this.rowsDataContext == null) {
            throw new IllegalStateException("Rows data was not resolved yet");
        } else {
            return this.rowsDataContext;
        }
    }

    @NotNull
    protected SQLQueryRowsSourceContext getRowsSources() {
        if (this.rowsSourceContext == null) {
            throw new IllegalStateException("Rows sources were not resolved yet");
        } else {
            return this.rowsSourceContext;
        }
    }

    @Override
    protected void applyContext(@NotNull SQLQueryDataContext dataContext, @NotNull SQLQueryRecognitionContext recognitionContext) {
        this.propagateContext(dataContext, recognitionContext);
    }

    /**
     * Propagate semantics context and establish relations through the query model by applying this model item's semantics
     */
    @NotNull
    public final SQLQueryDataContext propagateContext(
        @NotNull SQLQueryDataContext context,
        @NotNull SQLQueryRecognitionContext statistics
    ) {
        this.givenDataContext = context;
        return this.resultDataContext = this.propagateContextImpl(context, statistics);
    }

    @NotNull
    protected abstract SQLQueryDataContext propagateContextImpl(
        @NotNull SQLQueryDataContext context,
        @NotNull SQLQueryRecognitionContext statistics
    );

    /**
     * Propagate information about available tables down the model and about actually referenced tables back up
     */
    public final void resolveObjectAndRowsReferences(
        @NotNull SQLQueryRowsSourceContext context,
        @NotNull SQLQueryRecognitionContext statistics
    ) {
        this.resolveRowSources(context, statistics);
    }

    /**
     * Propagate information about available tables down the model and about actually referenced tables back up,
     * caching it in this rows source
     */
    @NotNull
    public final SQLQueryRowsSourceContext resolveRowSources(
        @NotNull SQLQueryRowsSourceContext context,
        @NotNull SQLQueryRecognitionContext statistics
    ) {
        return this.rowsSourceContext = this.resolveRowSourcesImpl(context, statistics);
    }

    protected abstract SQLQueryRowsSourceContext resolveRowSourcesImpl(
        @NotNull SQLQueryRowsSourceContext context,
        @NotNull SQLQueryRecognitionContext statistics
    );

    /**
     * Propagate information about values and row tuples across the query model
     */
    public final void resolveValueRelations(@NotNull SQLQueryRowsDataContext context, @NotNull SQLQueryRecognitionContext statistics) {
        traverseSubtreeSmart(
            this,
            SQLQueryRowsSourceModel.class,
            context,
            (n, c) -> n.resolveRowData(c, statistics),
            () -> statistics.getMonitor().isCanceled()
        );
    }

    protected final SQLQueryRowsDataContext resolveRowData(
        @NotNull SQLQueryRowsDataContext context,
        @NotNull SQLQueryRecognitionContext statistics
    ) {
        if (this.rowsDataContext == null) {
            this.rowsDataContext = this.resolveRowDataImpl(context, statistics);
        }
        return this.rowsDataContext;
    }

    protected abstract SQLQueryRowsDataContext resolveRowDataImpl(
        @NotNull SQLQueryRowsDataContext context,
        @NotNull SQLQueryRecognitionContext statistics
    );
}


