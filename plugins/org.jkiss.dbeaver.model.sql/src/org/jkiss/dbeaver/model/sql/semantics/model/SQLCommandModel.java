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
package org.jkiss.dbeaver.model.sql.semantics.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolEntry;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

import java.util.ArrayList;
import java.util.List;

public class SQLCommandModel extends SQLQueryModelContent {

    public static class VariableNode extends SQLQueryNodeModel {
        public final SQLQuerySymbolEntry name;
        public final String value;

        public VariableNode(SQLQuerySymbolEntry symbol, String value) {
            super(symbol.getSyntaxNode().getRealInterval(), symbol.getSyntaxNode());
            this.name = symbol;
            this.value = value;
        }

        @Override
        protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, T arg) {
            return visitor.visitCommandVariable(this, arg);
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
    }

    @NotNull
    private final String commandText;

    @NotNull
    private final List<VariableNode> variables = new ArrayList<>();

    public SQLCommandModel(@NotNull STMTreeNode fakeTree, @NotNull String commandText) {
        super(fakeTree.getRealInterval(), fakeTree);
        this.commandText = commandText;
    }

    @NotNull
    public String getCommandText() {
        return this.commandText;
    }

    @NotNull
    public VariableNode[] getVariables() {
        return this.variables.toArray(VariableNode[]::new);
    }

    public void addVariable(SQLQuerySymbolEntry symbol, String value) {
        this.variables.add(new VariableNode(symbol, value));
    }

    @Override
    protected void applyContext(@NotNull SQLQueryDataContext dataContext, @NotNull SQLQueryRecognitionContext recognitionContext) {
        // do nothing
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, T arg) {
        return visitor.visitCommand(this, arg);
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
}
