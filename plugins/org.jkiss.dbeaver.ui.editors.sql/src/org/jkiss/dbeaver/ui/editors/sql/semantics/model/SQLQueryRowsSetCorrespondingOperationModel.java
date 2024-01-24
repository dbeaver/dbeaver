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
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLQuerySymbolEntry;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SQLQueryResultTupleContext.SQLQueryResultColumn;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents INTERSECT, UNION and EXCEPT operation model in the context of SQL query semantics
 */
public class SQLQueryRowsSetCorrespondingOperationModel extends SQLQueryRowsSetOperationModel {
    private final List<SQLQuerySymbolEntry> correspondingColumnNames;
    private final SQLQueryRowsSetCorrespondingOperationKind kind;

    public SQLQueryRowsSetCorrespondingOperationModel(
        @NotNull Interval range,
        @NotNull SQLQueryRowsSourceModel left,
        @NotNull SQLQueryRowsSourceModel right,
        @NotNull List<SQLQuerySymbolEntry> correspondingColumnNames,
        @NotNull SQLQueryRowsSetCorrespondingOperationKind kind
    ) {
        super(range, left, right);
        this.correspondingColumnNames = correspondingColumnNames;
        this.kind = kind;
    }

    public SQLQueryRowsSetCorrespondingOperationKind getKind() {
        return this.kind;
    }

    @NotNull
    @Override
    protected SQLQueryDataContext propagateContextImpl(
        @NotNull SQLQueryDataContext context,
        @NotNull SQLQueryRecognitionContext statistics
    ) {
        SQLQueryDataContext left = this.left.propagateContext(context, statistics);
        SQLQueryDataContext right = this.right.propagateContext(context, statistics);

        List<SQLQueryResultColumn> resultColumns;
        boolean nonMatchingColumnSets = false;
        if (correspondingColumnNames.isEmpty()) { // require left and right to have the same tuples
            List<SQLQueryResultColumn> leftColumns = left.getColumnsList();
            List<SQLQueryResultColumn> rightColumns = right.getColumnsList();
            resultColumns = new ArrayList<>(Math.max(leftColumns.size(), rightColumns.size()));

            for (int i = 0; i < resultColumns.size(); i++) {
                if (i >= leftColumns.size()) {
                    resultColumns.add(rightColumns.get(i));
                    nonMatchingColumnSets = true;
                } else if (i >= rightColumns.size()) {
                    resultColumns.add(leftColumns.get(i));
                    nonMatchingColumnSets = true;
                } else { // TODO validate corresponding names to be the same?
                    resultColumns.add(new SQLQueryResultColumn(leftColumns.get(i).symbol.merge(rightColumns.get(i).symbol), this, null, null));
                }
            }
        } else { // require left and right to have columns subset as given with correspondingColumnNames
            resultColumns = new ArrayList<>(correspondingColumnNames.size());
            for (int i = 0; i < resultColumns.size(); i++) {
                SQLQuerySymbolEntry column = correspondingColumnNames.get(i);
                if (column.isNotClassified()) {
                    SQLQueryResultColumn leftDef = left.resolveColumn(column.getName());
                    SQLQueryResultColumn rightDef = right.resolveColumn(column.getName());

                    if (leftDef == null || rightDef == null) {
                        nonMatchingColumnSets = true;
                    }

                    column.getSymbol().setDefinition(column); // TODO combine multiple definitions
                    resultColumns.add(new SQLQueryResultColumn(column.getSymbol(), this, null, null));
                }
            }
        }

        if (nonMatchingColumnSets) {
            statistics.appendError((STMTreeNode) null, "UNION, EXCEPT and INTERSECT require subsets column tuples to match"); // TODO detailed messages per column
        }

        return correspondingColumnNames.isEmpty() ? left : context.overrideResultTuple(resultColumns); // TODO multiple definitions per symbol
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T node) {
        return visitor.visitRowsSetCorrespondingOp(this, node);
    }
}

