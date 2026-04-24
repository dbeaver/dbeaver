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
package org.jkiss.dbeaver.model.sql.semantics.model.ddl;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.semantics.*;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryResultColumn;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsSourceContext;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModel;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.sql.semantics.model.expressions.SQLQueryValueColumnReferenceExpression;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

public class SQLQueryTableAlterActionSpec extends SQLQueryNodeModel {
    @NotNull
    private final SQLQueryTableAlterActionKind actionKind;
    @Nullable
    private final SQLQueryColumnSpec columnSpec;
    @Nullable
    private final SQLQuerySymbolEntry columnName;
    @Nullable
    private final SQLQueryTableConstraintSpec tableConstraintSpec;
    @Nullable
    private final SQLQueryComplexName tableConstraintName;

    protected SQLQueryTableAlterActionSpec(
        @NotNull STMTreeNode syntaxNode,
        @NotNull SQLQueryTableAlterActionKind actionKind,
        @Nullable SQLQueryColumnSpec columnSpec,
        @Nullable SQLQuerySymbolEntry columnName,
        @Nullable SQLQueryTableConstraintSpec tableConstraintSpec,
        @Nullable SQLQueryComplexName tableConstraintName
    ) {
        super(syntaxNode.getRealInterval(), syntaxNode, columnSpec, tableConstraintSpec);
        this.actionKind = actionKind;
        this.columnSpec = columnSpec;
        this.columnName = columnName;
        this.tableConstraintSpec = tableConstraintSpec;
        this.tableConstraintName = tableConstraintName;
    }

    @NotNull
    public SQLQueryTableAlterActionKind getActionKind() {
        return this.actionKind;
    }

    @Nullable
    public SQLQueryColumnSpec getColumnSpec() {
        return this.columnSpec;
    }

    @Nullable
    public SQLQueryTableConstraintSpec getTableConstraintSpec() {
        return this.tableConstraintSpec;
    }

    /**
     * Propagate semantics context and establish relations through the query model
     */
    public void resolveRelations(
        @NotNull SQLQueryRowsSourceContext dataContext,
        @Nullable SQLQueryRowsDataContext tableContext,
        @NotNull SQLQueryRecognitionContext statistics
    ) {
        SQLQuerySymbolOrigin columnRefOrigin = tableContext == null ? null : new SQLQuerySymbolOrigin.ColumnNameFromRowsData(tableContext);

        if (this.columnSpec != null) {
            SQLQuerySymbolEntry columnRef = columnSpec.getColumnName();
            if (columnRef != null) {
                SQLQuerySymbol columnName = columnRef.getSymbol();
                if (columnRef.isNotClassified()) {
                    if (tableContext != null) {
                        SQLQueryResultColumn rc = tableContext.resolveColumn(statistics.getMonitor(), columnName.getName());
                        SQLQuerySemanticUtils.propagateColumnDefinition(columnRef, rc, statistics, columnRefOrigin);
                    } else {
                        columnName.setDefinition(columnRef);
                        columnName.setSymbolClass(SQLQuerySymbolClass.COLUMN);
                        columnRef.setOrigin(columnRefOrigin);
                    }
                }

                this.columnSpec.resolveRelations(dataContext, tableContext, statistics);
            }
        }

        if (this.columnName != null && this.columnName.isNotClassified()) {
            if (tableContext != null) {
                SQLQueryResultColumn rc = tableContext.resolveColumn(statistics.getMonitor(), this.columnName.getName());
                if (rc != null) {
                    SQLQuerySemanticUtils.propagateColumnDefinition(this.columnName, rc, statistics, columnRefOrigin);
                } else {
                    this.columnName.getSymbol().setSymbolClass(SQLQuerySymbolClass.COLUMN);
                    this.columnName.setOrigin(columnRefOrigin);
                    statistics.appendWarning(this.columnName, "Column " + this.columnName.getName() + " not found");
                }
            } else {
                // a message that the table doesn't exist will appear, so no need additional warning here, I assume
                this.columnName.getSymbol().setSymbolClass(SQLQuerySymbolClass.COLUMN);
            }
        }

        if (this.tableConstraintSpec != null) {
            this.tableConstraintSpec.resolveRelations(dataContext, tableContext, statistics);
        }

        if (this.tableConstraintName != null) {
            // TODO validate if constraint is missing and produce a warning
        }
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, T arg) {
        return visitor.visitAlterTableAction(this, arg);
    }
}
