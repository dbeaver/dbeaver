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
import org.jkiss.dbeaver.model.impl.struct.RelationalObjectType;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardParser;
import org.jkiss.dbeaver.model.sql.semantics.*;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryResultColumn;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsSourceContext;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryModelContent;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsTableValueModel;
import org.jkiss.dbeaver.model.stm.STMKnownRuleNames;
import org.jkiss.dbeaver.model.stm.STMTreeNode;
import org.jkiss.dbeaver.model.struct.DBSEntity;

import java.util.*;

public class SQLQueryTableCreateModel extends SQLQueryModelContent {

    @Nullable
    private final SQLQueryComplexName tableName;
    @NotNull
    private final List<SQLQueryColumnSpec> columns;
    @NotNull
    private final List<SQLQueryTableConstraintSpec> constraints;

    public SQLQueryTableCreateModel(
        @NotNull STMTreeNode syntaxNode,
        @Nullable SQLQueryComplexName tableName,
        @NotNull List<SQLQueryColumnSpec> columns,
        @NotNull List<SQLQueryTableConstraintSpec> constraints
    ) {
        super(syntaxNode.getRealInterval(), syntaxNode);
        this.tableName = tableName;
        this.columns = List.copyOf(columns);
        this.constraints = List.copyOf(constraints);

        this.columns.forEach(super::registerSubnode);
        this.constraints.forEach(super::registerSubnode);
    }

    @Nullable
    public SQLQueryComplexName getTableName() {
        return this.tableName;
    }

    @NotNull
    public List<SQLQueryColumnSpec> getColumns() {
        return this.columns;
    }

    @NotNull
    public List<SQLQueryTableConstraintSpec> getConstraints() {
        return this.constraints;
    }

    @Override
    public void resolveObjectAndRowsReferences(@NotNull SQLQueryRowsSourceContext context, @NotNull SQLQueryRecognitionContext statistics) {
        if (this.tableName != null && this.tableName.isNotClassified()) {
            List<DBSEntity> realTables = context.getConnectionInfo().findRealTables(statistics.getMonitor(), this.tableName.stringParts);
            DBSEntity realTable = realTables.size() == 1 ? realTables.getFirst() : null;

            SQLQuerySymbolOrigin nameOrigin = new SQLQuerySymbolOrigin.DbObjectRef(context, RelationalObjectType.TYPE_TABLE);
            if (realTable != null) {
                SQLQuerySemanticUtils.setNamePartsDefinition(this.tableName, realTable, nameOrigin);
            } else {
                SQLQuerySemanticUtils.performPartialResolution(
                    context,
                    statistics,
                    this.tableName,
                    nameOrigin,
                    Set.of(RelationalObjectType.TYPE_UNKNOWN),
                    SQLQuerySymbolClass.TABLE
                );
            }

            SQLQueryRowsTableValueModel virtualTableRows = new SQLQueryRowsTableValueModel(this.getSyntaxNode(), Collections.emptyList(), false);

            List<SQLQueryResultColumn> columns = new ArrayList<>(this.columns.size());
            for (SQLQueryColumnSpec columnSpec : this.columns) {
                SQLQuerySymbolEntry columnNameEntry = columnSpec.getColumnName();
                SQLQuerySymbol columnName;
                if (columnNameEntry != null) {
                    columnName = columnNameEntry.getSymbol();
                    if (columnNameEntry.isNotClassified()) {
                        columnName.setDefinition(columnNameEntry);
                        columnName.setSymbolClass(SQLQuerySymbolClass.COLUMN);
                    }
                } else {
                    columnName = new SQLQuerySymbol("?");
                }

                columns.add(new SQLQueryResultColumn(
                    columns.size(), columnName, virtualTableRows, null, null,
                    columnSpec.getDeclaredColumnType()
                ));
            }
            SQLQueryRowsDataContext tableContext = context.makeTuple(null, columns, Collections.emptyList());

            for (SQLQueryColumnSpec columnSpec : this.columns) {
                columnSpec.resolveRelations(context, tableContext, statistics);
            }

            for (SQLQueryTableConstraintSpec constraintSpec : this.constraints) {
                constraintSpec.resolveRelations(context, tableContext, statistics);
            }
        }

    }

    @Override
    public void resolveValueRelations(@NotNull SQLQueryRowsDataContext context, @NotNull SQLQueryRecognitionContext statistics) {
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, T arg) {
        return visitor.visitCreateTable(this, arg);
    }

    public static SQLQueryTableCreateModel recognize(SQLQueryModelRecognizer recognizer, STMTreeNode node) {
        SQLQueryComplexName tableName = recognizer.collectTableName(node);

        LinkedList<SQLQueryColumnSpec> columns = new LinkedList<>();
        LinkedList<SQLQueryTableConstraintSpec> constraints = new LinkedList<>();

        STMTreeNode elementsNode = node.findFirstChildOfName(STMKnownRuleNames.tableElementList);
        if (elementsNode != null) {
            for (STMTreeNode elementNode : elementsNode.findChildrenOfName(STMKnownRuleNames.tableElement)) {
                STMTreeNode payloadNode = elementNode.findFirstNonErrorChild();
                if (payloadNode != null) {
                    switch (payloadNode.getNodeKindId()) {
                        case SQLStandardParser.RULE_columnDefinition ->
                            columns.addLast(SQLQueryColumnSpec.recognize(recognizer, payloadNode));
                        case SQLStandardParser.RULE_tableConstraintDefinition ->
                            constraints.addLast(SQLQueryTableConstraintSpec.recognize(recognizer, payloadNode));
                    }
                }
            }
        }
        return new SQLQueryTableCreateModel(node, tableName, columns, constraints);
    }
}
