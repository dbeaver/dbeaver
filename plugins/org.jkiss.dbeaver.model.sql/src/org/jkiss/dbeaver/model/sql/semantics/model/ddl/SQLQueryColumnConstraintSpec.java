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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.sql.semantics.*;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryExprType;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryResultColumn;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModel;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.sql.semantics.model.expressions.SQLQueryValueColumnReferenceExpression;
import org.jkiss.dbeaver.model.sql.semantics.model.expressions.SQLQueryValueExpression;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsTableDataModel;
import org.jkiss.dbeaver.model.stm.STMTreeNode;
import org.jkiss.dbeaver.model.struct.*;

import java.util.*;
import java.util.stream.Collectors;

public class SQLQueryColumnConstraintSpec extends SQLQueryNodeModel {
    @Nullable
    private final SQLQueryQualifiedName constraintName;
    @NotNull
    private final SQLQueryColumnConstraintKind kind;

    @Nullable
    private final SQLQueryRowsTableDataModel referencedTable;
    @Nullable
    private final List<SQLQuerySymbolEntry> referencedColumns;
    @Nullable
    private final SQLQueryValueExpression checkExpression;

    public SQLQueryColumnConstraintSpec(
        @NotNull STMTreeNode syntaxNode,
        @Nullable SQLQueryQualifiedName constraintName,
        @NotNull SQLQueryColumnConstraintKind kind,
        @Nullable SQLQueryRowsTableDataModel referencedTable,
        @Nullable List<SQLQuerySymbolEntry> referencedColumns,
        @Nullable SQLQueryValueExpression checkExpression
    ) {
        super(syntaxNode.getRealInterval(), syntaxNode, checkExpression);
        this.constraintName = constraintName;
        this.kind = kind;
        this.referencedTable = referencedTable;
        this.referencedColumns = referencedColumns;
        this.checkExpression = checkExpression;
    }

    @Nullable
    public SQLQueryQualifiedName getConstraintName() {
        return this.constraintName;
    }

    @NotNull
    public SQLQueryColumnConstraintKind getKind() {
        return this.kind;
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

    /**
     * Propagate semantics context and establish relations through the query model
     */
    public void propagateContext(
        @NotNull SQLQueryDataContext dataContext,
        @Nullable SQLQueryDataContext tableContext,
        @NotNull SQLQueryRecognitionContext statistics
    ) {

        if (this.referencedTable != null) {
            SQLQueryDataContext referencedContext = propagateForReferencedEntity(this.referencedTable, this.referencedColumns, dataContext, statistics);
            if (referencedContext != null) {
                if (referencedContext.getColumnsList().size() != 1) {
                    statistics.appendWarning(this.getSyntaxNode(), "Inconsistent foreign key tuple size");
                }
            }
        }

        if (this.checkExpression != null && tableContext != null) {
            this.checkExpression.propagateContext(tableContext, statistics);
        }
    }

    /**
     * Propagate semantics context for referenced entity
     */
    public static @Nullable SQLQueryDataContext propagateForReferencedEntity(
        @NotNull SQLQueryRowsTableDataModel referencedTable,
        @Nullable List<SQLQuerySymbolEntry> referencedColumns,
        @NotNull SQLQueryDataContext dataContext,
        @NotNull SQLQueryRecognitionContext statistics
    ) {
        statistics.setTreatErrorAsWarnings(true);
        SQLQueryDataContext referencedContext = referencedTable.propagateContext(dataContext, statistics);
        statistics.setTreatErrorAsWarnings(false);
        DBSEntity realTable = referencedTable.getTable();
        SQLQueryDataContext resultContext;

        SQLQuerySymbolOrigin referencedColumnNameOrigin = new SQLQuerySymbolOrigin.ColumnNameFromContext(referencedContext);
        if (referencedColumns != null && !referencedColumns.isEmpty()) {
            List<SQLQueryResultColumn> resultColumns = new ArrayList<>(referencedColumns.size());
            if (realTable != null) {
                for (SQLQuerySymbolEntry columnRef : referencedColumns) {
                    SQLQueryResultColumn rc = referencedContext.resolveColumn(statistics.getMonitor(), columnRef.getName());
                    if (rc != null) {
                        if (columnRef.isNotClassified()) {
                            SQLQueryValueColumnReferenceExpression.propagateColumnDefinition(columnRef, rc, statistics, referencedColumnNameOrigin);
                        }
                        resultColumns.add(rc.withNewIndex(resultColumns.size()));
                    } else {
                        statistics.appendWarning(columnRef, "Failed to resolve column " + columnRef.getName());
                        columnRef.getSymbol().setSymbolClass(SQLQuerySymbolClass.COLUMN);
                        columnRef.setOrigin(referencedColumnNameOrigin);

                        resultColumns.add(new SQLQueryResultColumn(
                            resultColumns.size(), columnRef.getSymbol(),
                            referencedTable, realTable,
                            null, SQLQueryExprType.UNKNOWN
                        ));
                    }
                }
            } else {
                // table reference resolution failed, so cannot resolve its columns as well
                statistics.appendWarning(
                    referencedTable.getName().entityName,
                    "Failed to validate " + (referencedColumns.size() > 1 ? "compound " : "") +
                    "foreign key columns of table " + referencedTable.getName().toIdentifierString()
                );
                for (SQLQuerySymbolEntry columnRef : referencedColumns) {
                    if (columnRef.isNotClassified()) {
                        columnRef.getSymbol().setSymbolClass(SQLQuerySymbolClass.COLUMN);
                    }
                    resultColumns.add(new SQLQueryResultColumn(
                        resultColumns.size(), columnRef.getSymbol(),
                        referencedTable, referencedTable.getTable(),
                        null, SQLQueryExprType.UNKNOWN
                    ));
                }
            }
            resultContext = referencedContext.overrideResultTuple(null, resultColumns, Collections.emptyList());
        } else {
            if (realTable != null) {
                try {
                    Optional<? extends DBSEntityConstraint> pk = Optional.ofNullable(realTable.getConstraints(statistics.getMonitor()))
                        .orElse(Collections.emptyList()).stream()
                        .filter(c -> c.getConstraintType().equals(DBSEntityConstraintType.PRIMARY_KEY))
                        .findFirst();
                    if (pk.isPresent() && pk.get() instanceof DBSEntityReferrer referrer) {
                        List<DBSEntityAttribute> pkAttrs = Optional.ofNullable(referrer.getAttributeReferences(statistics.getMonitor()))
                            .orElse(Collections.emptyList()).stream()
                            .map(DBSEntityAttributeRef::getAttribute).collect(Collectors.toList());
                        if (pkAttrs.isEmpty()) {
                            statistics.appendWarning(
                                referencedTable.getName().entityName,
                                "Failed to obtain primary key attribute of the referenced table " + referencedTable.getName().toIdentifierString());
                            resultContext = null;
                        } else {
                            resultContext = referencedContext.overrideResultTuple(null, SQLQueryRowsTableDataModel.prepareResultColumnsList(
                                referencedTable.getName().entityName, referencedTable, realTable, referencedContext, statistics, pkAttrs
                            ));
                        }
                    } else {
                        statistics.appendWarning(
                            referencedTable.getName().entityName,
                            "Failed to obtain primary key of the referenced table " + referencedTable.getName().toIdentifierString());
                        resultContext = null;
                    }
                } catch (DBException e) {
                    statistics.appendError(
                        referencedTable.getName().entityName,
                        "Failed to resolve primary key of the referenced table " + referencedTable.getName().toIdentifierString(),
                        e
                    );
                    resultContext = null;
                }
            } else {
                // no explicit foreign key columns, and table is not resolved, so no way to validate the reference
                resultContext = null;
            }
        }

        return resultContext;
    }


}
