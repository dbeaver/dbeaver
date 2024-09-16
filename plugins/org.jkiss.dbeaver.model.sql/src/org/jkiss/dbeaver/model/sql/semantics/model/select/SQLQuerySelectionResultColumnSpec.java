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
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbol;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolClass;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolEntry;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryExprType;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryResultColumn;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.sql.semantics.model.expressions.SQLQueryValueExpression;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

import java.util.LinkedList;

/**
 * Describes one column of a selection result
 */
public class SQLQuerySelectionResultColumnSpec extends SQLQuerySelectionResultSublistSpec {
    @Nullable
    private final SQLQueryValueExpression valueExpression;
    @Nullable
    private final SQLQuerySymbolEntry alias;

    public SQLQuerySelectionResultColumnSpec(
        @NotNull SQLQuerySelectionResultModel resultModel,
        @NotNull STMTreeNode syntaxNode,
        @Nullable SQLQueryValueExpression valueExpression
    ) {
        this(resultModel, syntaxNode, valueExpression, null);
    }

    public SQLQuerySelectionResultColumnSpec(
        @NotNull SQLQuerySelectionResultModel resultModel,
        @NotNull STMTreeNode syntaxNode,
        @Nullable SQLQueryValueExpression valueExpression,
        @Nullable SQLQuerySymbolEntry alias
    ) {
        super(resultModel, syntaxNode);
        this.valueExpression = valueExpression;
        this.alias = alias;

        if (valueExpression != null) {
            this.registerSubnode(valueExpression);
        }
    }

    @Nullable
    public SQLQueryValueExpression getValueExpression() {
        return this.valueExpression;
    }

    @Nullable
    public SQLQuerySymbolEntry getAlias() {
        return this.alias;
    }

    @Override
    protected void collectColumns(
        @NotNull SQLQueryDataContext context,
        @NotNull SQLQueryRowsProjectionModel rowsSourceModel,
        @NotNull SQLQueryRecognitionContext statistics,
        @NotNull LinkedList<SQLQueryResultColumn> resultColumns
    ) {
        if (this.valueExpression != null) {
            this.valueExpression.propagateContext(context, statistics);
        }

        SQLQuerySymbol columnName;
        SQLQueryResultColumn underlyingColumn;
        if (this.alias != null) {
            if (this.alias.isNotClassified()) {
                columnName = this.alias.getSymbol();
                columnName.setDefinition(this.alias);
                columnName.setSymbolClass(SQLQuerySymbolClass.COLUMN_DERIVED);
            } else {
                return;
            }
            underlyingColumn = null;
        } else {
            if (this.valueExpression != null) {
                columnName = this.valueExpression.getColumnNameIfTrivialExpression();
                underlyingColumn = this.valueExpression.getColumnIfTrivialExpression();
            } else {
                columnName = null;
                underlyingColumn = null;
            }
            if (columnName == null) {
                columnName = new SQLQuerySymbol("?");
            }
        }

        SQLQueryExprType type = valueExpression == null ? SQLQueryExprType.UNKNOWN : valueExpression.getValueType();
        resultColumns.add(underlyingColumn == null
            ? new SQLQueryResultColumn(resultColumns.size(), columnName, rowsSourceModel, null, null, type)
            : new SQLQueryResultColumn(resultColumns.size(), columnName, rowsSourceModel, underlyingColumn.realSource, underlyingColumn.realAttr, type)
        );
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T node) {
        return visitor.visitSelectColumnSpec(this, node);
    }
}
