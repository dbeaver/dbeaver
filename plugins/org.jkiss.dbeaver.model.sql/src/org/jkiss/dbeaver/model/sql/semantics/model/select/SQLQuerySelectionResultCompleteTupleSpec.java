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
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolClass;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolOrigin;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryResultColumn;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsDataContext;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryTupleRefEntry;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

import java.util.LinkedList;

/**
 * Describes all columns of the table of a selection result
 */
public class SQLQuerySelectionResultCompleteTupleSpec extends SQLQuerySelectionResultSublistSpec {

    @NotNull
    private final SQLQueryTupleRefEntry tupleRefEntry;

    public SQLQuerySelectionResultCompleteTupleSpec(
        @NotNull STMTreeNode syntaxNode,
        @NotNull SQLQueryTupleRefEntry tupleRefEntry
    ) {
        super(syntaxNode);
        this.tupleRefEntry = tupleRefEntry;
    }

    @Nullable
    @Override
    public SQLQuerySymbolClass getAssociatedSymbolClass() {
        return null;
    }

    @Override
    protected void collectColumns(
        @NotNull SQLQueryRowsDataContext knownValues,
        @NotNull SQLQueryRowsProjectionModel rowsSourceModel,
        @NotNull SQLQueryRecognitionContext statistics,
        @NotNull LinkedList<SQLQueryResultColumn> resultColumns
    ) {
        this.tupleRefEntry.setOrigin(
            new SQLQuerySymbolOrigin.ExpandableRowsTupleRef(this.tupleRefEntry.getSyntaxNode(), knownValues, null)
        );
        this.collectForeignColumns(knownValues.getColumnsList(), rowsSourceModel, resultColumns);
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
        return visitor.visitSelectCompleteTupleSpec(this, arg);
    }
}