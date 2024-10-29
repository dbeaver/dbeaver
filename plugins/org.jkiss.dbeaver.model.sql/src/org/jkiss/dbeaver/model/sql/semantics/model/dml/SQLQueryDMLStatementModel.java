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
package org.jkiss.dbeaver.model.sql.semantics.model.dml;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryModelRecognizer;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryModelContent;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsTableDataModel;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

/**
 * Describes a statements operating with the table (INSERT, DELETE, ...)
 */
public abstract class SQLQueryDMLStatementModel extends SQLQueryModelContent {
    @Nullable
    private final SQLQueryRowsTableDataModel tableModel;
    @Nullable
    private SQLQueryDataContext givenContext = null;
    @Nullable
    private SQLQueryDataContext resultContext = null;
    
    public SQLQueryDMLStatementModel(
        @NotNull STMTreeNode syntaxNode,
        @Nullable SQLQueryRowsTableDataModel tableModel
    ) {
        super(syntaxNode.getRealInterval(), syntaxNode, tableModel);
        this.tableModel = tableModel;
    }

    @Nullable
    public SQLQueryRowsTableDataModel getTableModel() {
        return this.tableModel;
    }

    @Override
    protected final void applyContext(@NotNull SQLQueryDataContext context, @NotNull SQLQueryRecognitionContext statistics) {
        this.givenContext = context;
        if (this.tableModel != null) {
            this.resultContext = this.tableModel.propagateContext(context, statistics);
            this.propagateContextImpl(this.resultContext, statistics);
        }
    }

    @Nullable
    @Override
    public SQLQueryDataContext getGivenDataContext() {
        return this.givenContext;
    }

    @Nullable
    @Override
    public SQLQueryDataContext getResultDataContext() {
        return this.resultContext;
    }

    protected abstract void propagateContextImpl(@NotNull SQLQueryDataContext context, @NotNull SQLQueryRecognitionContext statistics);
}
