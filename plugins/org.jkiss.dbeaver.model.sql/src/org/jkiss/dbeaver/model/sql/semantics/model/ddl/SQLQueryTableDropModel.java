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
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryModelRecognizer;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryModelContent;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsTableDataModel;
import org.jkiss.dbeaver.model.stm.STMKnownRuleNames;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Describes DELETE statement
 */
public class SQLQueryTableDropModel extends SQLQueryModelContent {

    @Nullable
    private final List<SQLQueryRowsTableDataModel> tables;
    private final boolean isView;
    private final boolean ifExists;

    @Nullable
    private SQLQueryDataContext dataContext = null;

    @NotNull
    public static SQLQueryModelContent recognize(
        @NotNull SQLQueryModelRecognizer recognizer,
        @NotNull STMTreeNode node,
        boolean isView
    ) {
        List<SQLQueryRowsTableDataModel> tables = node.findChildrenOfName(STMKnownRuleNames.tableName).stream()
            .map(n -> recognizer.collectTableReference(n, true)).collect(Collectors.toList());
        boolean ifExists = node.findFirstChildOfName(STMKnownRuleNames.ifExistsSpec) != null; // "IF EXISTS" presented
        return new SQLQueryTableDropModel(node, tables, ifExists, isView);
    }

    private SQLQueryTableDropModel(
        @NotNull STMTreeNode syntaxNode,
        @Nullable List<SQLQueryRowsTableDataModel> tables,
        boolean ifExists,
        boolean isView) {
        super(syntaxNode.getRealInterval(), syntaxNode);
        this.tables = tables;
        this.ifExists = ifExists;
        this.isView = isView;
    }

    @Nullable
    public List<SQLQueryRowsTableDataModel> getTables() {
        return this.tables;
    }

    public boolean getIfExists() {
        return this.ifExists;
    }

    public boolean isView() {
        return isView;
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
        if (ifExists) {
            recognitionContext.setTreatErrorAsWarnings(true);
        }
        this.tables.forEach(t -> t.propagateContext(dataContext, recognitionContext));
        if (ifExists) {
            recognitionContext.setTreatErrorAsWarnings(false);
        }
    }

    @Nullable
    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
        return visitor.visitTableStatementDrop(this, arg);
    }

}
