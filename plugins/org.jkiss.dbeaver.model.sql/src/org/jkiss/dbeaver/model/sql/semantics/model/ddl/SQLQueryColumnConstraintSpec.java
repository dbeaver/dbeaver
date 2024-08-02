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
package org.jkiss.dbeaver.model.sql.semantics.model.ddl;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryQualifiedName;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolClass;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolEntry;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryResultColumn;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModel;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.sql.semantics.model.expressions.SQLQueryValueColumnReferenceExpression;
import org.jkiss.dbeaver.model.sql.semantics.model.expressions.SQLQueryValueExpression;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsTableDataModel;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

import java.util.List;

public class SQLQueryColumnConstraintSpec extends SQLQueryNodeModel {
    @NotNull
    private final SQLQueryQualifiedName constraintName;
    @NotNull
    private final SQLQueryColumnConstraintKind kind;

    @Nullable
    private final SQLQueryRowsTableDataModel referencedTable;
    @Nullable
    private final List<SQLQuerySymbolEntry> referencedColumns;
    @Nullable
    private final SQLQueryValueExpression checkExpression;

    public SQLQueryColumnConstraintSpec(@NotNull STMTreeNode syntaxNode, @NotNull SQLQueryQualifiedName constraintName, @NotNull SQLQueryColumnConstraintKind kind, @Nullable SQLQueryRowsTableDataModel referencedTable, @Nullable List<SQLQuerySymbolEntry> referencedColumns, @Nullable SQLQueryValueExpression checkExpression) {
        super(syntaxNode.getRealInterval(), syntaxNode);
        this.constraintName = constraintName;
        this.kind = kind;
        this.referencedTable = referencedTable;
        this.referencedColumns = referencedColumns;
        this.checkExpression = checkExpression;
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, T arg) {
        return visitor.visitColumnConstraintSpec(this, arg);
    }

    @Nullable
    @Override
    public SQLQueryDataContext getGivenDataContext() {
        return null;
    }

    @Nullable
    @Override
    public SQLQueryDataContext getResultDataContext() {
        return null;
    }

    public void propagateContext(SQLQueryDataContext tableContext, SQLQueryRecognitionContext statistics) {
        SQLQueryDataContext referencedContext = this.referencedTable == null ? null : this.referencedTable.propagateContext(tableContext, statistics);

        if (this.referencedColumns != null) {
            if (referencedContext != null) {
                for (SQLQuerySymbolEntry columnRef: this.referencedColumns) {
                    SQLQueryResultColumn rc = referencedContext.resolveColumn(statistics.getMonitor(), columnRef.getName());
                    SQLQueryValueColumnReferenceExpression.propagateColumnDefinition(columnRef, rc, statistics);
                }
            } else {
                // table reference resolution failed, so cannot resolve its columns as well
                for (SQLQuerySymbolEntry columnRef: this.referencedColumns) {
                    columnRef.getSymbol().setSymbolClass(SQLQuerySymbolClass.ERROR);
                }
            }
        }

        if (this.checkExpression != null) {
            this.checkExpression.propagateContext(tableContext, statistics);
        }
    }
}
