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
package org.jkiss.dbeaver.model.sql.semantics.model.dml;

import org.antlr.v4.runtime.misc.Interval;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryLexicalScope;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryModelRecognizer;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolEntry;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryResultColumn;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryModelContent;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.sql.semantics.model.expressions.SQLQueryValueColumnReferenceExpression;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsSourceModel;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsTableDataModel;
import org.jkiss.dbeaver.model.stm.STMKnownRuleNames;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

import java.util.Collections;
import java.util.List;

/**
 * Describes INSERT statement
 */
public class SQLQueryInsertModel extends SQLQueryDMLStatementModel {
    @Nullable
    private final List<SQLQuerySymbolEntry> columnNames;
    @Nullable
    private final SQLQueryRowsSourceModel valuesRows;
    @Nullable
    private final SQLQueryLexicalScope columnsScope;

    @NotNull
    public static SQLQueryModelContent recognize(@NotNull SQLQueryModelRecognizer recognizer, @NotNull STMTreeNode node) {
        STMTreeNode tableNameNode = node.findFirstChildOfName(STMKnownRuleNames.tableName);
        SQLQueryRowsTableDataModel tableModel = tableNameNode == null ? null : recognizer.collectTableReference(tableNameNode, false);

        List<SQLQuerySymbolEntry> columnNames;
        SQLQueryRowsSourceModel valuesRows;
        SQLQueryLexicalScope insertColumnsScope;

        STMTreeNode insertColumnsAndSource = node.findFirstChildOfName(STMKnownRuleNames.insertColumnsAndSource);
        if (insertColumnsAndSource != null) {
            STMTreeNode insertColumnList = insertColumnsAndSource.findFirstChildOfName(STMKnownRuleNames.insertColumnList);
            columnNames = insertColumnList == null ? null : recognizer.collectColumnNameList(insertColumnList);

            STMTreeNode valuesNode = insertColumnsAndSource.findFirstChildOfName(STMKnownRuleNames.queryExpression);
            valuesRows = valuesNode == null ? null : recognizer.collectQueryExpression(valuesNode);

            int columnsScopeFrom = insertColumnsAndSource.getRealInterval().a;
            int columnsScopeTo = valuesNode == null ? insertColumnsAndSource.getRealInterval().b : valuesNode.getRealInterval().a;

            insertColumnsScope = new SQLQueryLexicalScope();
            insertColumnsScope.setInterval(Interval.of(columnsScopeFrom, columnsScopeTo));
        } else {
            columnNames = Collections.emptyList();
            valuesRows = null; // use default table?
            insertColumnsScope = null;
        }

        return new SQLQueryInsertModel(node, tableModel, columnNames, valuesRows, insertColumnsScope);
    }

    private SQLQueryInsertModel(
        @NotNull STMTreeNode syntaxNode,
        @Nullable SQLQueryRowsTableDataModel tableModel,
        @Nullable List<SQLQuerySymbolEntry> columnNames,
        @Nullable SQLQueryRowsSourceModel valuesRows,
        @Nullable SQLQueryLexicalScope columnsScope) {
        super(syntaxNode, tableModel);
        this.columnNames = columnNames;
        this.valuesRows = valuesRows;
        this.columnsScope = columnsScope;
        if (columnsScope != null) {
            this.registerLexicalScope(columnsScope);
        }
    }

    @Nullable
    public List<SQLQuerySymbolEntry> getColumnNames() {
        return this.columnNames;
    }

    @Nullable
    public SQLQueryRowsSourceModel getValuesRows() {
        return this.valuesRows;
    }

    @Override
    public void propagateContextImpl(@NotNull SQLQueryDataContext context, @NotNull SQLQueryRecognitionContext statistics) {
        if (this.columnsScope != null) {
            this.columnsScope.setContext(context);
        }
        if (this.columnNames != null) {
            for (SQLQuerySymbolEntry columnName : this.columnNames) {
                if (columnName.isNotClassified()) {
                    SQLQueryResultColumn column = context.resolveColumn(statistics.getMonitor(), columnName.getName());
                    if (column != null || !context.hasUndresolvedSource()) {
                        SQLQueryValueColumnReferenceExpression.propagateColumnDefinition(columnName, column, statistics);
                    }
                }
            }
        }

        if (this.valuesRows != null) {
            SQLQueryDataContext valuesContext = this.valuesRows.propagateContext(context, statistics);
            // TODO validate column tuples consistency
        }
    }

    @Nullable
    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
        return visitor.visitTableStatementInsert(this, arg);
    }
}
