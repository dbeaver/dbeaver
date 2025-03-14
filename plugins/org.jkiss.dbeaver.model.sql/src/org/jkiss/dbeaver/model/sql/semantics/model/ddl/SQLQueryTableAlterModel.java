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
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardParser;
import org.jkiss.dbeaver.model.sql.semantics.*;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryModelContent;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsTableDataModel;
import org.jkiss.dbeaver.model.stm.STMKnownRuleNames;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

import java.util.*;

public class SQLQueryTableAlterModel extends SQLQueryModelContent {

    private static final Map<String, SQLQueryTableAlterActionKind> alterActionKindByNodeName = Map.of(
        STMKnownRuleNames.addColumnDefinition, SQLQueryTableAlterActionKind.ADD_COLUMN,
        STMKnownRuleNames.alterColumnDefinition, SQLQueryTableAlterActionKind.ALTER_COLUMN,
        STMKnownRuleNames.renameColumnDefinition, SQLQueryTableAlterActionKind.RENAME_COLUMN,
        STMKnownRuleNames.dropColumnDefinition, SQLQueryTableAlterActionKind.DROP_COLUMN,
        STMKnownRuleNames.addTableConstraintDefinition, SQLQueryTableAlterActionKind.ADD_TABLE_CONSTRAINT,
        STMKnownRuleNames.dropTableConstraintDefinition, SQLQueryTableAlterActionKind.DROP_TABLE_CONSTRAINT
    );

    @Nullable
    private final SQLQueryRowsTableDataModel targetTable;

    @NotNull
    private final List<SQLQueryTableAlterActionSpec> alterActions;

    @Nullable
    private SQLQueryDataContext dataContext = null;

    public SQLQueryTableAlterModel(
        @NotNull STMTreeNode syntaxNode,
        @Nullable SQLQueryRowsTableDataModel targetTable,
        @NotNull List<SQLQueryTableAlterActionSpec> alterActions
    ) {
        super(syntaxNode.getRealInterval(), syntaxNode);
        this.targetTable = targetTable;
        this.alterActions = List.copyOf(alterActions);

        this.alterActions.forEach(this::registerSubnode);
    }

    @Nullable
    public SQLQueryRowsTableDataModel getTargetTable() {
        return this.targetTable;
    }

    @NotNull
    public List<SQLQueryTableAlterActionSpec> getAlterActions() {
        return this.alterActions;
    }

    @Override
    protected void applyContext(@NotNull SQLQueryDataContext dataContext, @NotNull SQLQueryRecognitionContext statistics) {
        this.dataContext = dataContext;

        if (targetTable != null) {
            SQLQueryDataContext tableContext = this.targetTable.propagateContext(dataContext, statistics);
            for (SQLQueryTableAlterActionSpec alterAction : this.alterActions) {
                alterAction.propagateContext(dataContext, this.targetTable.getTable() == null ? null : tableContext, statistics);
            }
        } else {
            statistics.appendWarning(this.getSyntaxNode(), "Missing table name");
        }
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, T arg) {
        return visitor.visitAlterTable(this, arg);
    }

    @Nullable
    @Override
    public SQLQueryDataContext getGivenDataContext() {
        return this.dataContext;
    }

    @Nullable
    @Override
    public SQLQueryDataContext getResultDataContext() {
        return this.dataContext;
    }

    public static SQLQueryTableAlterModel recognize(SQLQueryModelRecognizer recognizer, STMTreeNode node) {
        SQLQueryRowsTableDataModel targetTable = recognizer.collectTableReference(node, true);

        LinkedList<SQLQueryTableAlterActionSpec> alterActions = new LinkedList<>();

        for (STMTreeNode subnode : node.findChildrenOfName(STMKnownRuleNames.alterTableAction)) {
            STMTreeNode actionNode = subnode.findFirstNonErrorChild();
            if (actionNode != null) {
                SQLQueryTableAlterActionKind actionKind = alterActionKindByNodeName.get(actionNode.getNodeName());

                SQLQueryColumnSpec columnSpec = null;
                SQLQuerySymbolEntry columnName = null;
                SQLQueryTableConstraintSpec tableConstraintSpec = null;
                SQLQueryQualifiedName tableConstraintName = null;

                if (actionKind != null) {
                    switch (actionKind) {
                        case ADD_COLUMN ->
                            columnSpec = Optional.ofNullable(actionNode.findFirstChildOfName(STMKnownRuleNames.columnDefinition))
                                .map(n -> SQLQueryColumnSpec.recognize(recognizer, n)).orElse(null);
                        case ALTER_COLUMN, RENAME_COLUMN, DROP_COLUMN ->
                            columnName = Optional.ofNullable(actionNode.findFirstChildOfName(STMKnownRuleNames.columnName))
                                .map(n -> recognizer.collectIdentifier(n, null)).orElse(null);
                        case ADD_TABLE_CONSTRAINT -> tableConstraintSpec =
                            Optional.ofNullable(actionNode.findFirstChildOfName(STMKnownRuleNames.tableConstraintDefinition))
                                .map(n -> SQLQueryTableConstraintSpec.recognize(recognizer, n)).orElse(null);
                        case DROP_TABLE_CONSTRAINT ->
                            tableConstraintName = Optional.ofNullable(actionNode.findFirstChildOfName(STMKnownRuleNames.constraintName))
                                .map(recognizer::collectQualifiedName).orElse(null);
                    }

                    alterActions.addLast(new SQLQueryTableAlterActionSpec(
                        actionNode, actionKind, columnSpec, columnName, tableConstraintSpec, tableConstraintName
                    ));
                }
            }
        }

        return new SQLQueryTableAlterModel(node, targetTable, alterActions);
    }
}
