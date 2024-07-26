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

import org.antlr.v4.runtime.misc.Interval;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryModelRecognizer;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;
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
     * Returns result data context, if it has been resolved. Otherwise, throws UnsupportedOperationException.
     */
    @NotNull
    public SQLQueryDataContext getResultDataContext() {
        if (this.resultDataContext == null) {
            throw new IllegalStateException("Data context was not resolved for the rows source yet");
        } else {
            return this.resultDataContext;
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


}


