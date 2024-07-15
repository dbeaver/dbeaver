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
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryResultColumn;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModel;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

import java.util.LinkedList;
import java.util.List;

/**
 * Describes a fragment of the selection result
 */
public abstract class SQLQuerySelectionResultSublistSpec extends SQLQueryNodeModel {

    @NotNull
    private final SQLQuerySelectionResultModel resultModel;

    protected SQLQuerySelectionResultSublistSpec(@NotNull SQLQuerySelectionResultModel resultModel, @NotNull STMTreeNode syntaxNode) {
        super(syntaxNode.getRealInterval(), syntaxNode);
        this.resultModel = resultModel;
    }

    @Nullable
    @Override
    public SQLQueryDataContext getGivenDataContext() {
        return resultModel.getResultDataContext();
    }

    @Nullable
    @Override
    public SQLQueryDataContext getResultDataContext() {
        return resultModel.getResultDataContext();
    }

    @NotNull
    protected abstract void collectColumns(
        @NotNull SQLQueryDataContext context,
        @NotNull SQLQueryRowsProjectionModel rowsSourceModel,
        @NotNull SQLQueryRecognitionContext statistics,
        @NotNull LinkedList<SQLQueryResultColumn> resultColumns
    );

    protected void collectForeignColumns(
        @NotNull List<SQLQueryResultColumn> foreignColumns,
        @NotNull SQLQueryRowsProjectionModel rowsSourceModel,
        @NotNull LinkedList<SQLQueryResultColumn> resultColumns
    ) {
        for (SQLQueryResultColumn c : foreignColumns) {
            resultColumns.addLast(new SQLQueryResultColumn(resultColumns.size(), c.symbol, rowsSourceModel, c.realSource, c.realAttr, c.type));
        }
    }
}