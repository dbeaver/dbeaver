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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.editors.sql.semantics.*;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SQLQueryExprType;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SQLQueryResultTupleContext.SQLQueryResultColumn;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SourceResolutionResult;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SQLQuerySelectionResultModel extends SQLQueryNodeModel {

    private final List<ResultSublistSpec> sublists;

    public SQLQuerySelectionResultModel(@NotNull Interval range, int capacity) {
        super(range);
        this.sublists = new ArrayList<>(capacity);
    }

    public List<ResultSublistSpec> getSublists() {
        return this.sublists;
    }

    public void addColumnSpec(@NotNull Interval range, @NotNull SQLQueryValueExpression valueExpression) {
        this.sublists.add(new ColumnSpec(range, valueExpression));
    }

    public void addColumnSpec(
        @NotNull Interval range,
        @NotNull SQLQueryValueExpression valueExpression,
        @Nullable SQLQuerySymbolEntry alias
    ) {
        this.sublists.add(new ColumnSpec(range, valueExpression, alias));
    }

    public void addTupleSpec(@NotNull Interval range, @NotNull SQLQueryValueTupleReferenceExpression tupleRef) {
        this.sublists.add(new TupleSpec(range, tupleRef));
    }

    public void addCompleteTupleSpec(@NotNull Interval range) {
        this.sublists.add(new CompleteTupleSpec(range));
    }

    public List<SQLQueryResultColumn> expandColumns(
        @NotNull SQLQueryDataContext context,
        @NotNull SQLQueryRowsProjectionModel rowsSourceModel,
        @NotNull SQLQueryRecognitionContext statistics
    ) {
        return this.sublists.stream().flatMap(s -> s.expand(context, rowsSourceModel, statistics)).collect(Collectors.toList());
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T node) {
        return visitor.visitSelectionResult(this, node);
    }

    public static abstract class ResultSublistSpec extends SQLQueryNodeModel {

        protected ResultSublistSpec(Interval region) {
            super(region);
        }

        @NotNull
        protected abstract Stream<SQLQueryResultColumn> expand(
            @NotNull SQLQueryDataContext context,
            @NotNull SQLQueryRowsProjectionModel rowsSourceModel,
            @NotNull SQLQueryRecognitionContext statistics
        );
    }

    public static class ColumnSpec extends ResultSublistSpec {
        private final SQLQueryValueExpression valueExpression;
        private final SQLQuerySymbolEntry alias;

        public ColumnSpec(@NotNull Interval region, @NotNull SQLQueryValueExpression valueExpression) {
            this(region, valueExpression, null);
        }

        public ColumnSpec(@NotNull Interval region, @NotNull SQLQueryValueExpression valueExpression, @Nullable SQLQuerySymbolEntry alias) {
            super(region);
            this.valueExpression = valueExpression;
            this.alias = alias;
        }

        @NotNull
        public SQLQueryValueExpression getValueExpression() {
            return this.valueExpression;
        }

        @Nullable
        public SQLQuerySymbolEntry getAlias() {
            return this.alias;
        }

        @NotNull
        @Override
        protected Stream<SQLQueryResultColumn> expand(
            @NotNull SQLQueryDataContext context,
            @NotNull SQLQueryRowsProjectionModel rowsSourceModel,
            @NotNull SQLQueryRecognitionContext statistics
        ) {
            this.valueExpression.propagateContext(context, statistics);

            SQLQuerySymbol columnName;
            SQLQueryResultColumn underlyingColumn;
            if (this.alias != null) {
                if (this.alias.isNotClassified()) {
                    columnName = this.alias.getSymbol();
                    columnName.setDefinition(this.alias);
                    columnName.setSymbolClass(SQLQuerySymbolClass.COLUMN_DERIVED);
                } else {
                    return Stream.empty();
                }
                underlyingColumn = null;
            } else {
                columnName = this.valueExpression.getColumnNameIfTrivialExpression();
                underlyingColumn = this.valueExpression.getColumnIfTrivialExpression();
                if (columnName == null) {
                    columnName = new SQLQuerySymbol("?");
                }
            }

            SQLQueryExprType type = valueExpression.getValueType();
            return Stream.of(underlyingColumn == null
                ? new SQLQueryResultColumn(columnName, rowsSourceModel, null, null, type)
                : new SQLQueryResultColumn(columnName, rowsSourceModel, underlyingColumn.realSource, underlyingColumn.realAttr, type)
            );
        }

        @Override
        protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T node) {
            return visitor.visitSelectColumnSpec(this, node);
        }
    }

    public static class TupleSpec extends ResultSublistSpec {
        private final SQLQueryValueTupleReferenceExpression tupleReference;
        
        public TupleSpec(@NotNull Interval region, @NotNull SQLQueryValueTupleReferenceExpression tupleReference) {
            super(region);
            this.tupleReference = tupleReference;
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
        protected Stream<SQLQueryResultColumn> expand(
            @NotNull SQLQueryDataContext context,
            @NotNull SQLQueryRowsProjectionModel rowsSourceModel,
            @NotNull SQLQueryRecognitionContext statistics
        ) {
            this.tupleReference.propagateContext(context, statistics);
            
            SQLQueryRowsSourceModel tupleSource = this.tupleReference.getTupleSource();
            if (tupleSource != null) {
                return tupleSource.getDataContext().getColumnsList().stream();
            } else {
                return Stream.empty();
            }
        }

        @Override
        protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T node) {
            return visitor.visitSelectTupleSpec(this, node);
        }
    }

    public static class CompleteTupleSpec extends ResultSublistSpec {

        public CompleteTupleSpec(@NotNull Interval region) {
            super(region);
        }

        @NotNull
        @Override
        protected Stream<SQLQueryResultColumn> expand(
            @NotNull SQLQueryDataContext context,
            @NotNull SQLQueryRowsProjectionModel rowsSourceModel,
            @NotNull SQLQueryRecognitionContext statistics
        ) {
            return context.getColumnsList().stream();
        }

        @Override
        protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
            return visitor.visitSelectCompleteTupleSpec(this, arg);
        }
    }
}
