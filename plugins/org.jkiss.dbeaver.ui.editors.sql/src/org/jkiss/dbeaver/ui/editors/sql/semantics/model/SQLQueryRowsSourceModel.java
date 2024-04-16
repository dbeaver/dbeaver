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
import org.jkiss.dbeaver.model.stm.STMTreeNode;
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SQLQueryDataContext;


public abstract class SQLQueryRowsSourceModel extends SQLQueryModelContent {
    private SQLQueryDataContext givenDataContext = null;
    private SQLQueryDataContext resultDataContext = null;

    public SQLQueryRowsSourceModel(@NotNull STMTreeNode syntaxNode, SQLQueryNodeModel ... subnodes) {
        super(syntaxNode.getRealInterval(), syntaxNode, subnodes);
    }

    public SQLQueryRowsSourceModel(@NotNull Interval region, STMTreeNode syntaxNode, SQLQueryNodeModel ... subnodes) {
        super(region, syntaxNode, subnodes);
    }
    
    @Override
    public SQLQueryDataContext getGivenDataContext() {
        return this.givenDataContext;
    }

    @NotNull
    public SQLQueryDataContext getResultDataContext() {
        if (this.resultDataContext == null) {
            throw new UnsupportedOperationException("Data context was not resolved for the rows source yet");
        } else {
            return this.resultDataContext;
        }
    }
    
    @Override
    void applyContext(@NotNull SQLQueryDataContext dataContext, @NotNull SQLQueryRecognitionContext recognitionContext) {
        this.propagateContext(dataContext, recognitionContext);
    }

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


