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
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolEntry;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryResultColumn;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModel;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.sql.semantics.model.expressions.SQLQueryValueExpression;
import org.jkiss.dbeaver.model.sql.semantics.model.expressions.SQLQueryValueTupleReferenceExpression;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Describes the structure of the result of select clause
 */
public class SQLQuerySelectionResultModel extends SQLQueryNodeModel {

    @NotNull
    private final List<SQLQuerySelectionResultSublistSpec> sublists;
    @Nullable
    private SQLQueryDataContext dataContext = null;

    public SQLQuerySelectionResultModel(@NotNull STMTreeNode syntaxNode, int capacity) {
        super(syntaxNode.getRealInterval(), syntaxNode);
        this.sublists = new ArrayList<>(capacity);
    }

    @NotNull
    public List<SQLQuerySelectionResultSublistSpec> getSublists() {
        return this.sublists;
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
    
    private void registerSublist(SQLQuerySelectionResultSublistSpec sublist) {
        this.sublists.add(sublist);
        super.registerSubnode(sublist);
    }

    /**
     * Add single column to the selection result model
     */
    public void addColumnSpec(@NotNull STMTreeNode syntaxNode, @Nullable SQLQueryValueExpression valueExpression) {
        this.registerSublist(new SQLQuerySelectionResultColumnSpec(this, syntaxNode, valueExpression));
    }


    /**
     * Add single column with alias to the selection result model
     */
    public void addColumnSpec(
        @NotNull STMTreeNode syntaxNode,
        @Nullable SQLQueryValueExpression valueExpression,
        @Nullable SQLQuerySymbolEntry alias
    ) {
        this.registerSublist(new SQLQuerySelectionResultColumnSpec(this, syntaxNode, valueExpression, alias));
    }


    /**
     * Add several columns of some table to the selection result model
     */
    public void addTupleSpec(@NotNull STMTreeNode syntaxNode, @NotNull SQLQueryValueTupleReferenceExpression tupleRef) {
        this.registerSublist(new SQLQuerySelectionResultTupleSpec(this, syntaxNode, tupleRef));
    }

    /**
     * Add all columns of some table to the selection result model
     */
    public void addCompleteTupleSpec(@NotNull STMTreeNode syntaxNode) {
        this.registerSublist(new SQLQuerySelectionResultCompleteTupleSpec(this, syntaxNode));
    }

    /**
     * Prepare a list of result columns
     */
    @NotNull
    public List<SQLQueryResultColumn> expandColumns(
        @NotNull SQLQueryDataContext context,
        @NotNull SQLQueryRowsProjectionModel rowsSourceModel,
        @NotNull SQLQueryRecognitionContext statistics
    ) {
        this.dataContext = context;
        LinkedList<SQLQueryResultColumn> resultColumns = new LinkedList<>();
        for (SQLQuerySelectionResultSublistSpec sublist : this.sublists) {
            sublist.collectColumns(context, rowsSourceModel, statistics, resultColumns);
        }
        return List.copyOf(resultColumns);
    }

    @Nullable
    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T node) {
        return visitor.visitSelectionResult(this, node);
    }
}
