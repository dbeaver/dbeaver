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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.sql.semantics.*;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryExprType;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryResultColumn;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsSourceContext;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModel;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.sql.semantics.model.expressions.SQLQueryValueExpression;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsTableDataModel;
import org.jkiss.dbeaver.model.stm.STMTreeNode;
import org.jkiss.dbeaver.model.struct.*;

import java.util.*;
import java.util.stream.Collectors;

public class SQLQueryColumnConstraintSpec extends SQLQueryNodeModel {
    @Nullable
    private final SQLQueryComplexName constraintName;
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
        @Nullable SQLQueryComplexName constraintName,
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
    public SQLQueryComplexName getConstraintName() {
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

    /**
     * Propagate semantics context and establish relations through the query model
     */
    public void resolveRelations(
        @NotNull SQLQueryRowsSourceContext rowsContext,
        @Nullable SQLQueryRowsDataContext tableDataContext,
        @NotNull SQLQueryRecognitionContext statistics
    ) {

        if (this.referencedTable != null) {
            SQLQueryRowsDataContext referencedContext = propagateForReferencedEntity(this.referencedTable, this.referencedColumns, rowsContext, statistics);
            if (referencedContext != null) {
                if (referencedContext.getColumnsList().size() != 1) {
                    statistics.appendWarning(this.getSyntaxNode(), "Inconsistent foreign key tuple size");
                }
            }
        }

        if (this.checkExpression != null && tableDataContext != null) {
            this.checkExpression.resolveRowSources(tableDataContext.getRowsSources(), statistics);
            this.checkExpression.resolveValueRelations(tableDataContext, statistics);
        }
    }

    /**
     * Propagate semantics context for referenced entity
     */
    @Nullable
    public static SQLQueryRowsDataContext propagateForReferencedEntity(
        @NotNull SQLQueryRowsTableDataModel referencedTable,
        @Nullable List<SQLQuerySymbolEntry> referencedColumns,
        @NotNull SQLQueryRowsSourceContext rowsContext,
        @NotNull SQLQueryRecognitionContext statistics
    ) {
        statistics.setTreatErrorAsWarnings(true);
        referencedTable.resolveObjectAndRowsReferences(rowsContext, statistics);
        referencedTable.resolveValueRelations(rowsContext.makeEmptyTuple(), statistics);
        SQLQueryRowsDataContext referencedContext = referencedTable.getRowsDataContext();
        statistics.setTreatErrorAsWarnings(false);
        DBSEntity realTable = referencedTable.getTable();
        SQLQueryRowsDataContext resultContext;

        SQLQuerySymbolOrigin referencedColumnNameOrigin = new SQLQuerySymbolOrigin.ColumnNameFromRowsData(referencedContext);
        if (referencedColumns != null && !referencedColumns.isEmpty()) {
            List<SQLQueryResultColumn> resultColumns = new ArrayList<>(referencedColumns.size());
            if (realTable != null) {
                for (SQLQuerySymbolEntry columnRef : referencedColumns) {
                    SQLQueryResultColumn rc = referencedContext.resolveColumn(statistics.getMonitor(), columnRef.getName());
                    if (rc != null) {
                        if (columnRef.isNotClassified()) {
                            SQLQuerySemanticUtils.propagateColumnDefinition(columnRef, rc, statistics, referencedColumnNameOrigin);
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
                    referencedTable.getName().syntaxNode,
                    "Failed to validate " + (referencedColumns.size() > 1 ? "compound " : "") +
                    "foreign key columns of table " + referencedTable.getName().getNameString()
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
            resultContext = referencedContext.getRowsSources().makeTuple(null, resultColumns, Collections.emptyList());
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
                                referencedTable.getName().syntaxNode,
                                "Failed to obtain primary key attribute of the referenced table " + referencedTable.getName().getNameString()
                            );
                            resultContext = null;
                        } else {
                            resultContext = referencedContext.getRowsSources().makeTuple(null, SQLQuerySemanticUtils.prepareResultColumnsList(
                                referencedTable.getName().syntaxNode, referencedTable, realTable, referencedContext.getConnection().dialect, statistics, pkAttrs
                            ));
                        }
                    } else {
                        statistics.appendWarning(
                            referencedTable.getName().syntaxNode,
                            "Failed to obtain primary key of the referenced table " + referencedTable.getName().getNameString()
                        );
                        resultContext = null;
                    }
                } catch (DBException e) {
                    statistics.appendError(
                        referencedTable.getName().syntaxNode,
                        "Failed to resolve primary key of the referenced table " + referencedTable.getName().getNameString(),
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
