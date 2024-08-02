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

import org.antlr.v4.runtime.misc.Interval;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryModelRecognizer;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolEntry;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModel;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

public class SQLQueryTableConstraintSpec extends SQLQueryNodeModel {
    @Nullable
    private final SQLQuerySymbolEntry constraintName;

    protected SQLQueryTableConstraintSpec(@NotNull STMTreeNode syntaxNode, @Nullable SQLQuerySymbolEntry constraintName) {
        super(syntaxNode.getRealInterval(), syntaxNode);
        this.constraintName = constraintName;
    }

    public void propagateContext(SQLQueryDataContext tableContext, SQLQueryRecognitionContext statistics) {

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

    public static SQLQueryTableConstraintSpec recognizer(SQLQueryModelRecognizer recognizer, STMTreeNode node) {
        // TODO
        return new SQLQueryTableConstraintSpec(node, null);
    }

}
