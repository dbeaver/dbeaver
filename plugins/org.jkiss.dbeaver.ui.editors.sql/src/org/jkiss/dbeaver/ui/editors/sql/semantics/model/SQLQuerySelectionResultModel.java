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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.editors.sql.semantics.*;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SourceResolutionResult;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SQLQuerySelectionResultModel {

    private abstract class ResultSublistSpec {
        @NotNull
        protected abstract Stream<SQLQuerySymbol> expand(@NotNull SQLQueryDataContext context, @NotNull SQLQueryRecognitionContext statistics);
    }

    private class ColumnSpec extends ResultSublistSpec {
        private final SQLQueryValueExpression valueExpression;
        private final SQLQuerySymbolEntry alias;

        public ColumnSpec(@NotNull SQLQueryValueExpression valueExpression) {
            this(valueExpression, null);
        }

        public ColumnSpec(@NotNull SQLQueryValueExpression valueExpression, @Nullable SQLQuerySymbolEntry alias) {
            this.valueExpression = valueExpression;
            this.alias = alias;
        }

        @NotNull
        @Override
        protected Stream<SQLQuerySymbol> expand(@NotNull SQLQueryDataContext context, @NotNull SQLQueryRecognitionContext statistics) {
            this.valueExpression.propagateContext(context, statistics);

            SQLQuerySymbol columnName;
            if (this.alias != null) {
                columnName = this.alias.getSymbol();
                columnName.setDefinition(this.alias);
                columnName.setSymbolClass(SQLQuerySymbolClass.COLUMN_DERIVED);
            } else {
                columnName = this.valueExpression.getColumnNameIfTrivialExpression();
                if (columnName == null) {
                    columnName = new SQLQuerySymbol("?");
                }
            }

            return Stream.of(columnName);
        }
    }

    private class TupleSpec extends ResultSublistSpec {
        private final SQLQueryQualifiedName tableName;

        public TupleSpec(SQLQueryQualifiedName tableName) {
            this.tableName = tableName;
        }

        @NotNull
        @Override
        protected Stream<SQLQuerySymbol> expand(@NotNull SQLQueryDataContext context, @NotNull SQLQueryRecognitionContext statistics) {
            SourceResolutionResult rr = context.resolveSource(this.tableName.toListOfStrings()); // TODO consider multiple joins of one table
            if (rr != null) {
                this.tableName.setDefinition(rr);
                return rr.source.getDataContext().getColumnsList().stream();
            } else {
                this.tableName.setSymbolClass(SQLQuerySymbolClass.ERROR);
                statistics.appendError(this.tableName.entityName, "The table doesn't participate in this subquery context");
                return Stream.empty();
            }
        }
    }

    private class CompleteTupleSpec extends ResultSublistSpec {
        public CompleteTupleSpec() {
            // do nothing
        }

        @NotNull
        @Override
        protected Stream<SQLQuerySymbol> expand(@NotNull SQLQueryDataContext context, @NotNull SQLQueryRecognitionContext statistics) {
            return context.getColumnsList().stream();
        }
    }


    private final List<ResultSublistSpec> sublists;

    public SQLQuerySelectionResultModel(int capacity) {
        this.sublists = new ArrayList<>(capacity);
    }

    public void addColumnSpec(@NotNull SQLQueryValueExpression valueExpression) {
        this.sublists.add(new ColumnSpec(valueExpression));
    }

    public void addColumnSpec(@NotNull SQLQueryValueExpression valueExpression, @Nullable SQLQuerySymbolEntry alias) {
        this.sublists.add(new ColumnSpec(valueExpression, alias));
    }

    public void addTupleSpec(SQLQueryQualifiedName tableName) {
        this.sublists.add(new TupleSpec(tableName));
    }

    public void addCompleteTupleSpec() {
        this.sublists.add(new CompleteTupleSpec());
    }

    public List<SQLQuerySymbol> expandColumns(@NotNull SQLQueryDataContext context, @NotNull SQLQueryRecognitionContext statistics) {
        return this.sublists.stream().flatMap(s -> s.expand(context, statistics)).collect(Collectors.toList());
    }
}
