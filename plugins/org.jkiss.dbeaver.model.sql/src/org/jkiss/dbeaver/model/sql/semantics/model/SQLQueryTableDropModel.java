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
package org.jkiss.dbeaver.model.sql.semantics.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryModelContext;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

import java.util.List;

/**
 * Describes DELETE statement
 */
public class SQLQueryTableDropModel extends SQLQueryModelContent {

    private final List<SQLQueryRowsTableDataModel> tables;
    private final boolean ifExists;

    @Nullable
    private SQLQueryDataContext dataContext = null;

/*
    public static SQLQueryTableDropModel createModel(@NotNull SQLQueryModelContext context, @NotNull STMTreeNode node) {
        List<SQLQueryRowsTableDataModel> tables = new ArrayList<>(node.getChildCount());
        for (STMTreeNode tableNameNode : node.getChildren()) {
            if (tableNameNode.getNodeName().equals(STMKnownRuleNames.tableName)) {
                tables.add(context.collectTableReference(tableNameNode));
            }
        }
        boolean ifExists = node.findChildOfName(STMKnownRuleNames.ifExistsSpec) != null; // "IF EXISTS" presented
        return new SQLQueryTableDropModel(node, tables, ifExists);
    }
*/

    public SQLQueryTableDropModel(
        @NotNull SQLQueryModelContext context,
        @NotNull STMTreeNode syntaxNode,
        @Nullable List<SQLQueryRowsTableDataModel> tables,
        boolean ifExists
    ) {
        super(context, syntaxNode.getRealInterval(), syntaxNode);
        this.tables = tables;
        this.ifExists = ifExists;
    }

    public List<SQLQueryRowsTableDataModel> getTables() {
        return this.tables;
    }

    public boolean getIfExists() {
        return this.ifExists;
    }

    @Nullable
    @Override
    public SQLQueryDataContext getResultDataContext() {
        return this.dataContext;
    }

    @Nullable
    @Override
    public SQLQueryDataContext getGivenDataContext() {
        return this.dataContext;
    }

    @Override
    protected void applyContext(@NotNull SQLQueryDataContext dataContext, @NotNull SQLQueryRecognitionContext recognitionContext) {
        this.dataContext = dataContext;
        this.tables.forEach(t -> t.propagateContext(dataContext, recognitionContext));
    }

    @Nullable
    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
        return visitor.visitTableStatementDrop(this, arg);
    }
}
