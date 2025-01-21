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
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryResultColumn;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryModelContent;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsTableValueModel;
import org.jkiss.dbeaver.model.stm.STMKnownRuleNames;
import org.jkiss.dbeaver.model.stm.STMTreeNode;
import org.jkiss.dbeaver.model.struct.DBSEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class SQLQueryTableCreateModel extends SQLQueryModelContent {

    @Nullable
    private final SQLQueryQualifiedName tableName;
    @NotNull
    private final List<SQLQueryColumnSpec> columns;
    @NotNull
    private final List<SQLQueryTableConstraintSpec> constraints;

    @Nullable
    private SQLQueryDataContext dataContext = null;

    public SQLQueryTableCreateModel(
        @NotNull STMTreeNode syntaxNode,
        @Nullable SQLQueryQualifiedName tableName,
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
    public SQLQueryQualifiedName getTableName() {
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
    protected void applyContext(@NotNull SQLQueryDataContext dataContext, @NotNull SQLQueryRecognitionContext statistics) {
        this.dataContext = dataContext;

        if (this.tableName != null && this.tableName.isNotClassified()) {
            List<String> nameStrings = this.tableName.toListOfStrings();
            DBSEntity realTable = dataContext.findRealTable(statistics.getMonitor(), nameStrings);

            if (realTable != null) {
                this.tableName.setDefinition(realTable, new SQLQuerySymbolOrigin.DbObjectFromContext(dataContext));
            } else {
                this.tableName.setSymbolClass(SQLQuerySymbolClass.TABLE);
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
            SQLQueryDataContext tableContext = dataContext.overrideResultTuple(null, columns, Collections.emptyList());

            for (SQLQueryColumnSpec columnSpec : this.columns) {
                columnSpec.propagateContext(dataContext, tableContext, statistics);
            }

            for (SQLQueryTableConstraintSpec constraintSpec : this.constraints) {
                constraintSpec.propagateContext(dataContext, tableContext, statistics);
            }
        }
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, T arg) {
        return visitor.visitCreateTable(this, arg);
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

    public static SQLQueryTableCreateModel recognize(SQLQueryModelRecognizer recognizer, STMTreeNode node) {
        SQLQueryQualifiedName tableName = recognizer.collectTableName(node);

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
