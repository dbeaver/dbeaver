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
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryQualifiedName;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryResultColumn;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.sql.semantics.model.expressions.SQLQueryValueTupleReferenceExpression;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

import java.util.LinkedList;

/**
 * Describes several columns from the table of a selection result
 */
public class SQLQuerySelectionResultTupleSpec extends SQLQuerySelectionResultSublistSpec {
    @NotNull
    private final SQLQueryValueTupleReferenceExpression tupleReference;

    public SQLQuerySelectionResultTupleSpec(
        @NotNull SQLQuerySelectionResultModel resultModel,
        @NotNull STMTreeNode syntaxNode,
        @NotNull SQLQueryValueTupleReferenceExpression tupleReference
    ) {
        super(resultModel, syntaxNode);
        this.tupleReference = tupleReference;
        this.registerSubnode(tupleReference);
    }

    @NotNull
    public SQLQueryQualifiedName getTableName() {
        return this.tupleReference.getTableName();
    }

    @Nullable
    public SQLQueryRowsSourceModel getTupleSource() {
        return this.tupleReference.getTupleSource();
    }

    @NotNull
    @Override
    protected void collectColumns(
            @NotNull SQLQueryDataContext context,
            @NotNull SQLQueryRowsProjectionModel rowsSourceModel,
            @NotNull SQLQueryRecognitionContext statistics,
            @NotNull LinkedList<SQLQueryResultColumn> resultColumns
    ) {
        this.tupleReference.propagateContext(context, statistics);

        SQLQueryRowsSourceModel tupleSource = this.tupleReference.getTupleSource();
        if (tupleSource != null) {
            this.collectForeignColumns(tupleSource.getResultDataContext().getColumnsList(), rowsSourceModel, resultColumns);
        }
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T node) {
        return visitor.visitSelectTupleSpec(this, node);
    }
}
