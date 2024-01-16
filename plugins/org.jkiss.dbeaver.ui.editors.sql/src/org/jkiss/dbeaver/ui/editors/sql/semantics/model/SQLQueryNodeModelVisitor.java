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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SQLQueryDummyDataSourceContext.DummyTableRowsSource;
import org.jkiss.dbeaver.ui.editors.sql.semantics.model.SQLQuerySelectionResultModel.ColumnSpec;
import org.jkiss.dbeaver.ui.editors.sql.semantics.model.SQLQuerySelectionResultModel.CompleteTupleSpec;
import org.jkiss.dbeaver.ui.editors.sql.semantics.model.SQLQuerySelectionResultModel.TupleSpec;

public interface SQLQueryNodeModelVisitor<T, R> {

    @Nullable
    R visitValueSubqueryExpr(SQLQueryValueSubqueryExpression subqueryExpr, T arg);

    @Nullable
    R visitValueFlatExpr(SQLQueryValueFlattenedExpression flattenedExpr, T arg);

    @Nullable
    R visitValueColumnRefExpr(SQLQueryValueColumnReferenceExpression columnRefExpr, T arg);

    @Nullable
    R visitSelectionResult(SQLQuerySelectionResultModel selectionResult, T arg);

    @Nullable
    R visitSelectionModel(SQLQuerySelectionModel selection, T arg);

    @Nullable
    R visitRowsTableData(SQLQueryRowsTableDataModel tableData, T arg);

    @Nullable
    R visitRowsTableValue(SQLQueryRowsTableValueModel tableValue, T arg);

    @Nullable
    R visitRowsCrossJoin(SQLQueryRowsCrossJoinModel crossJoin, T arg);

    @Nullable
    R visitRowsCorrelatedSource(SQLQueryRowsCorrelatedSourceModel correlated, T arg);

    @Nullable
    R visitRowsNaturalJoin(SQLQueryRowsNaturalJoinModel naturalJoin, T arg);

    @Nullable
    R visitRowsProjection(@NotNull SQLQueryRowsProjectionModel projection, @NotNull T arg);

    @Nullable
    R visitRowsSetCorrespondingOp(@NotNull SQLQueryRowsSetCorrespondingOperationModel correspondingOp, @NotNull T arg);

    @Nullable
    R visitDummyTableRowsSource(@NotNull DummyTableRowsSource dummyTable, @NotNull T arg);

    @Nullable
    R visitSelectCompleteTupleSpec(@NotNull CompleteTupleSpec completeTupleSpec, @NotNull T arg);

    @Nullable
    R visitSelectTupleSpec(@NotNull TupleSpec tupleSpec, @NotNull T arg);

    @Nullable
    R visitSelectColumnSpec(@NotNull ColumnSpec columnSpec, @NotNull T arg);
}
