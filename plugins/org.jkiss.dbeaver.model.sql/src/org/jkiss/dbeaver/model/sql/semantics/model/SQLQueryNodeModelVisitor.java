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
package org.jkiss.dbeaver.model.sql.semantics.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDummyDataSourceContext.DummyTableRowsSource;
import org.jkiss.dbeaver.model.sql.semantics.model.ddl.*;
import org.jkiss.dbeaver.model.sql.semantics.model.dml.*;
import org.jkiss.dbeaver.model.sql.semantics.model.expressions.*;
import org.jkiss.dbeaver.model.sql.semantics.model.select.*;

/**
 * Query model visitor. Each new model class of a top-level of the query abstraction should have the corresponding method here.
 */
public interface SQLQueryNodeModelVisitor<T, R> {

    /**
     * Visit subquery expression
     */
    @Nullable
    R visitValueSubqueryExpr(@NotNull SQLQueryValueSubqueryExpression subqueryExpr, T arg);

    /**
     * Visit value expressions tree
     */
    @Nullable
    R visitValueFlatExpr(@NotNull SQLQueryValueFlattenedExpression flattenedExpr, T arg);

    /**
     * Visit script variable
     */
    @Nullable
    R visitValueVariableExpr(@NotNull SQLQueryValueVariableExpression varExpr, T arg);

    /**
     * Visit column reference
     */
    @Nullable
    R visitValueColumnRefExpr(SQLQueryValueColumnReferenceExpression columnRefExpr, T arg);

    /**
     * Visit reference to the tuple of columns of the table
     */
    @Nullable
    R visitValueTupleRefExpr(@NotNull SQLQueryValueTupleReferenceExpression tupleRefExpr, T arg);

    /**
     * Visit a member access to the element of the composite type
     */
    @Nullable
    R visitValueMemberReferenceExpr(@NotNull SQLQueryValueMemberExpression memberRefExpr, T arg);

    /**
     * Visit array element(s) access
     */
    @Nullable
    R visitValueIndexingExpr(@NotNull SQLQueryValueIndexingExpression indexingExpr, T arg);

    /**
     * Visit type cast expression
     */
    @Nullable
    R visitValueTypeCastExpr(@NotNull SQLQueryValueTypeCastExpression typeCastExpr, T arg);

    /**
     * Visit constant expression
     */
    @Nullable
    R visitValueConstantExpr(@NotNull SQLQueryValueConstantExpression constExpr, T arg);

    /**
     * Visit select clause result
     */
    @Nullable
    R visitSelectionResult(@NotNull SQLQuerySelectionResultModel selectionResult, T arg);

    /**
     * Visit query
     */
    @Nullable
    R visitSelectionModel(@NotNull SQLQueryModel selection, T arg);

    /**
     * Visit table definition
     */
    @Nullable
    R visitRowsTableData(@NotNull SQLQueryRowsTableDataModel tableData, T arg);

    /**
     * Visit a table constructed by VALUES clause
     */
    @Nullable
    R visitRowsTableValue(@NotNull SQLQueryRowsTableValueModel tableValue, T arg);

    /**
     * Visit cross join clause
     */
    @Nullable
    R visitRowsCrossJoin(@NotNull SQLQueryRowsCrossJoinModel crossJoin, T arg);

    /**
     * Visit a subquery source that have an alias and optionally columns list
     */
    @Nullable
    R visitRowsCorrelatedSource(@NotNull SQLQueryRowsCorrelatedSourceModel correlated, T arg);

    /**
     * Visit natral join
     */
    @Nullable
    R visitRowsNaturalJoin(@NotNull SQLQueryRowsNaturalJoinModel naturalJoin, T arg);

    /**
     * Visit select clause
     */
    @Nullable
    R visitRowsProjection(@NotNull SQLQueryRowsProjectionModel projection, T arg);

    /**
     * Visit set operation kind
     */
    @Nullable
    R visitRowsSetCorrespondingOp(@NotNull SQLQueryRowsSetCorrespondingOperationModel correspondingOp, T arg);

    /**
     * Visit table definition
     */
    @Nullable
    R visitDummyTableRowsSource(@NotNull DummyTableRowsSource dummyTable, T arg);

    /**
     * Visit all columns of the table of a selection result
     */
    @Nullable
    R visitSelectCompleteTupleSpec(@NotNull SQLQuerySelectionResultCompleteTupleSpec completeTupleSpec, T arg);

    /**
     * Visit several columns from the table of a selection result
     */
    @Nullable
    R visitSelectTupleSpec(@NotNull SQLQuerySelectionResultTupleSpec tupleSpec, T arg);

    /**
     * Visit one column of a selection result
     */
    @Nullable
    R visitSelectColumnSpec(@NotNull SQLQuerySelectionResultColumnSpec columnSpec, T arg);

    /**
     * Visit Common Table Expressiion (CTE)
     */
    @Nullable
    R visitRowsCte(@NotNull SQLQueryRowsCteModel cte, T arg);

    /**
     * Visit Common table expression (CTE) subquery
     */
    @Nullable
    R visitRowsCteSubquery(@NotNull SQLQueryRowsCteSubqueryModel cteSubquery, T arg);

    /**
     * Visit DELETE statement
     */
    @Nullable
    R visitTableStatementDelete(@NotNull SQLQueryDeleteModel deleteStatement, T arg);

    /**
     * Visit INSERT statement
     */
    @Nullable
    R visitTableStatementInsert(@NotNull SQLQueryInsertModel insertStatement, T arg);

    /**
     * Visit UPDATE statement
     */
    @Nullable
    R visitTableStatementUpdate(@NotNull SQLQueryUpdateModel updateStatement, T arg);

    /**
     * Visit SET clause of a UPDATE statement
     */
    @Nullable
    R visitTableStatementUpdateSetClause(@NotNull SQLQueryUpdateSetClauseModel setClause, T arg);

    @Nullable
    R visitTableStatementDrop(@NotNull SQLQueryTableDropModel dropStatement, T arg);

    @Nullable
    R visitObjectStatementDrop(@NotNull SQLQueryObjectDropModel dropStatement, T arg);

    R visitObjectReference(SQLQueryObjectDataModel objectReference, T arg);

    R visitCreateTable(SQLQueryTableCreateModel createTable, T arg);

    R visitColumnConstraintSpec(SQLQueryColumnConstraintSpec columnConstraintSpec, T arg);

    R visitColumnSpec(SQLQueryColumnSpec columnSpec, T arg);

    R visitTableConstraintSpec(SQLQueryTableConstraintSpec tableConstraintSpec, T arg);

    R visitAlterTable(SQLQueryTableAlterModel alterTable, T arg);

    R visitAlterTableAction(SQLQueryTableAlterActionSpec actionSpec, T arg);

    R visitRowsProjectionInto(SQLQuerySelectIntoModel selectIntoStatement, T arg);

    R visitRowsProjectionIntoTargetsList(SQLQuerySelectIntoModel.SQLQuerySelectIntoTargetsList targetsList, T arg);
}
