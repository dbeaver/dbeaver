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
import org.jkiss.dbeaver.model.sql.semantics.*;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryResultColumn;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModel;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.sql.semantics.model.expressions.SQLQueryValueColumnReferenceExpression;
import org.jkiss.dbeaver.model.sql.semantics.model.expressions.SQLQueryValueExpression;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsTableDataModel;
import org.jkiss.dbeaver.model.stm.STMKnownRuleNames;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

import java.util.*;

public class SQLQueryTableConstraintSpec extends SQLQueryNodeModel {

    private static final Map<String, SQLQueryTableConstraintKind> constraintKindByNodeName = Map.of(
        STMKnownRuleNames.uniqueConstraintDefinition, SQLQueryTableConstraintKind.UNIQUE,
        STMKnownRuleNames.referentialConstraintDefinition, SQLQueryTableConstraintKind.REFERENCES,
        STMKnownRuleNames.checkConstraintDefinition, SQLQueryTableConstraintKind.CHECK
    );

    @Nullable
    private final SQLQueryQualifiedName constraintName;
    @NotNull
    private final SQLQueryTableConstraintKind constraintKind;

    @Nullable
    private final List<SQLQuerySymbolEntry> tupleColumnsList;

    @Nullable
    private final SQLQueryRowsTableDataModel referencedTable;
    @Nullable
    private final List<SQLQuerySymbolEntry> referencedColumns;
    @Nullable
    private final SQLQueryValueExpression checkExpression;


    protected SQLQueryTableConstraintSpec(
        @NotNull STMTreeNode syntaxNode,
        @Nullable SQLQueryQualifiedName constraintName,
        @NotNull SQLQueryTableConstraintKind constraintKind,
        @Nullable List<SQLQuerySymbolEntry> tupleColumnsList,
        @Nullable SQLQueryRowsTableDataModel referencedTable,
        @Nullable List<SQLQuerySymbolEntry> referencedColumns,
        @Nullable SQLQueryValueExpression checkExpression
    ) {
        super(syntaxNode.getRealInterval(), syntaxNode, checkExpression);
        this.constraintName = constraintName;
        this.constraintKind = constraintKind;
        this.tupleColumnsList = tupleColumnsList;
        this.referencedTable = referencedTable;
        this.referencedColumns = referencedColumns;
        this.checkExpression = checkExpression;
    }

    @Nullable
    public SQLQueryQualifiedName getConstraintName() {
        return constraintName;
    }

    @NotNull
    public SQLQueryTableConstraintKind getConstraintKind() {
        return constraintKind;
    }

    /**
     * Propagate semantics context and establish relations through the query model
     */
    public void propagateContext(
        @NotNull SQLQueryDataContext sourceContext,
        @Nullable SQLQueryDataContext tableContext,
        @NotNull SQLQueryRecognitionContext statistics
    ) {
        List<SQLQueryResultColumn> referenceKey;
        if (this.tupleColumnsList != null && !this.tupleColumnsList.isEmpty() && tableContext != null) {
            SQLQuerySymbolOrigin columnNameOrigin = new SQLQuerySymbolOrigin.ColumnNameFromContext(tableContext);
            referenceKey = new ArrayList<>(this.tupleColumnsList.size());
            for (SQLQuerySymbolEntry columnRef : this.tupleColumnsList) {
                if (columnRef.isNotClassified()) {
                    SQLQueryResultColumn rc = tableContext.resolveColumn(statistics.getMonitor(), columnRef.getName());
                    if (rc != null) {
                        SQLQueryValueColumnReferenceExpression.propagateColumnDefinition(columnRef, rc, statistics, columnNameOrigin);
                    } else {
                        columnRef.getSymbol().setSymbolClass(SQLQuerySymbolClass.COLUMN);
                        statistics.appendWarning(columnRef, "Column " + columnRef.getName() + " not found");
                    }
                    referenceKey.add(rc);
                }
            }
        } else {
            referenceKey = null;
        }

        if (this.referencedTable != null) {
            SQLQueryDataContext referencedContext = SQLQueryColumnConstraintSpec.propagateForReferencedEntity(
                this.referencedTable,
                this.referencedColumns,
                sourceContext,
                statistics
            );
            if (referencedContext != null && referenceKey != null) {
                List<SQLQueryResultColumn> referencedKey = referencedContext.getColumnsList();
                if (referenceKey.size() != referencedKey.size()) {
                    statistics.appendError(this.getSyntaxNode(), "Inconsistent foreign key tuple size");
                }
                // TODO validate data types of tupleColumnList against referencedContext tuple
            }
        }

        if (this.checkExpression != null && tableContext != null) {
            this.checkExpression.propagateContext(tableContext, statistics);
        }
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, T arg) {
        return visitor.visitTableConstraintSpec(this, arg);
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


    @NotNull
    public static SQLQueryTableConstraintSpec recognize(@NotNull SQLQueryModelRecognizer recognizer, @NotNull STMTreeNode node) {
        SQLQueryQualifiedName constraintName = Optional.ofNullable(node.findFirstChildOfName(STMKnownRuleNames.constraintNameDefinition))
            .map(n -> n.findFirstChildOfName(STMKnownRuleNames.constraintName))
            .map(recognizer::collectQualifiedName).orElse(null);

        STMTreeNode constraintNode = Optional.ofNullable(node.findFirstChildOfName(STMKnownRuleNames.tableConstraint))
            .map(n -> n.findFirstNonErrorChild()).orElse(null);

        SQLQueryTableConstraintKind constraintKind;
        List<SQLQuerySymbolEntry> tupleColumnsList = null;
        SQLQueryRowsTableDataModel referencedTable = null;
        List<SQLQuerySymbolEntry> referencedColumns = null;
        SQLQueryValueExpression checkExpression = null;
        if (constraintNode != null) {
            constraintKind = Optional.ofNullable(constraintKindByNodeName.get(constraintNode.getNodeName()))
                .orElse(SQLQueryTableConstraintKind.UNKNOWN);

            tupleColumnsList = switch (constraintKind) {
                case UNIQUE -> recognizer.collectColumnNameList(constraintNode);
                case REFERENCES -> Optional.ofNullable(constraintNode.findFirstChildOfName(STMKnownRuleNames.referencingColumns))
                    .map(recognizer::collectColumnNameList).orElse(null);
                default -> null;
            };

            switch (constraintKind) {
                case CHECK -> checkExpression = recognizer.collectValueExpression(constraintNode);
                case REFERENCES -> {
                    STMTreeNode refNode = Optional.ofNullable(constraintNode.findFirstChildOfName(STMKnownRuleNames.referencesSpecification))
                        .map(n -> n.findFirstChildOfName(STMKnownRuleNames.referencedTableAndColumns))
                        .orElse(null);
                    if (refNode != null) {
                        referencedTable = recognizer.collectTableReference(refNode, false); // TODO consider if FK to alias allowed
                        referencedColumns = recognizer.collectColumnNameList(refNode);
                    }
                }
            }
        } else {
            constraintKind = SQLQueryTableConstraintKind.UNKNOWN;
        }
        return new SQLQueryTableConstraintSpec(node, constraintName, constraintKind, tupleColumnsList, referencedTable, referencedColumns, checkExpression);
    }

}
