/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SQLQueryDummyDataSourceContext.DummyTableRowsSource;
import org.jkiss.dbeaver.ui.editors.sql.semantics.model.SQLQuerySelectionResultModel.ColumnSpec;
import org.jkiss.dbeaver.ui.editors.sql.semantics.model.SQLQuerySelectionResultModel.CompleteTupleSpec;
import org.jkiss.dbeaver.ui.editors.sql.semantics.model.SQLQuerySelectionResultModel.TupleSpec;

public interface SQLQueryNodeModelVisitor<T, R> {

	R visitValueSubqueryExpr(SQLQueryValueSubqueryExpression subqueryExpr, T arg);

	R visitValueFlatExpr(SQLQueryValueFlattenedExpression flattenedExpr, T arg);

	R visitValueColumnRefExpr(SQLQueryValueColumnReferenceExpression columnRefExpr, T arg);

	R visitSelectionResult(SQLQuerySelectionResultModel selectionResult, T arg);

	R visitSelectionModel(SQLQuerySelectionModel selection, T arg);

	R visitRowsTableData(SQLQueryRowsTableDataModel tableData, T arg);

	R visitRowsTableValue(SQLQueryRowsTableValueModel tableValue, T arg);

	R visitRowsSelectionFilter(SQLQueryRowsSelectionFilterModel selectionFilter, T arg);

	R visitRowsCrossJoin(SQLQueryRowsCrossJoinModel crossJoin, T arg);

	R visitRowsCorrelatedSource(SQLQueryRowsCorrelatedSourceModel correlated, T arg);

	R visitRowsNaturalJoin(SQLQueryRowsNaturalJoinModel naturalJoin, T arg);

	R visitRowsProjection(SQLQueryRowsProjectionModel projection, T arg);

	R visitRowsSetCorrespondingOp(SQLQueryRowsSetCorrespondingOperationModel correspondingOp, T arg);

	R visitDummyTableRowsSource(DummyTableRowsSource dummyTable, T arg);

	R visitSelectCompleteTupleSpec(CompleteTupleSpec completeTupleSpec, T arg);

	R visitSelectTupleSpec(TupleSpec tupleSpec, T arg);

	R visitSelectColumnSpec(ColumnSpec columnSpec, T arg);
	
}
