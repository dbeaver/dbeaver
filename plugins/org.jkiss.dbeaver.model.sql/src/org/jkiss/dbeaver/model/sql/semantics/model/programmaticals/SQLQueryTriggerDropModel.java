/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.sql.semantics.model.programmaticals;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.impl.struct.RelationalObjectType;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryModelRecognizer;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsSourceContext;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.sql.semantics.model.ddl.SQLQueryObjectDropModel;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

import java.util.Set;

/** Body-free DROP TRIGGER using the shared object-drop model. */
public final class SQLQueryTriggerDropModel extends SQLQueryProgrammaticalStatementModel {
    @NotNull
    private final SQLQueryObjectDropModel drop;

    private SQLQueryTriggerDropModel(@NotNull STMTreeNode node, @NotNull SQLQueryObjectDropModel drop) {
        super(node, drop);
        this.drop = drop;
    }

    @NotNull
    public static SQLQueryTriggerDropModel recognize(@NotNull SQLQueryModelRecognizer recognizer, @NotNull STMTreeNode node) {
        return new SQLQueryTriggerDropModel(node, (SQLQueryObjectDropModel) SQLQueryObjectDropModel.recognize(
            recognizer, node, RelationalObjectType.TYPE_TRIGGER, Set.of()));
    }

    @NotNull
    public SQLQueryObjectDropModel getDrop() {
        return this.drop;
    }

    @Nullable
    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @Nullable T arg) {
        return visitor.visitTriggerDrop(this, arg);
    }

    @Override
    public void resolveObjectAndRowsReferences(
        @NotNull SQLQueryRowsSourceContext context, @NotNull SQLQueryRecognitionContext statistics
    ) {
        this.drop.resolveObjectAndRowsReferences(context, statistics);
    }

    @Override
    public boolean tryResolveValueRelations(
        @NotNull SQLQueryRowsDataContext context, @NotNull SQLQueryRecognitionContext statistics
    ) {
        return this.drop.tryResolveValueRelations(context, statistics);
    }
}
