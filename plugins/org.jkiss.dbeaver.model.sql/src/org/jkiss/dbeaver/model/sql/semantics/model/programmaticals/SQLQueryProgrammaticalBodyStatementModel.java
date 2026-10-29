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
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsSourceContext;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModel;
import org.jkiss.dbeaver.model.sql.semantics.model.statements.SQLQueryStatementModel;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

import java.util.Arrays;
import java.util.stream.Stream;

/** A program that can contain a procedural body, including an incomplete one with no body yet. */
public abstract class SQLQueryProgrammaticalBodyStatementModel extends SQLQueryProgrammaticalStatementModel {
    @Nullable
    private final SQLQueryStatementModel body;
    @Nullable
    private SQLQueryRowsSourceContext bodyContext;

    protected SQLQueryProgrammaticalBodyStatementModel(
        @NotNull STMTreeNode node, @Nullable SQLQueryStatementModel body, @NotNull SQLQueryNodeModel... otherChildren
    ) {
        super(node, Stream.concat(
            Stream.<SQLQueryNodeModel>of(body), Arrays.stream(otherChildren)
        ).toArray(SQLQueryNodeModel[]::new));
        this.body = body;
    }

    @Nullable
    public SQLQueryStatementModel getBody() {
        return this.body;
    }

    @NotNull
    protected SQLQueryRowsSourceContext prepareBodyContext(
        @NotNull SQLQueryRowsSourceContext context, @NotNull SQLQueryRecognitionContext statistics
    ) {
        return context;
    }

    @Override
    public final void resolveObjectAndRowsReferences(
        @NotNull SQLQueryRowsSourceContext context, @NotNull SQLQueryRecognitionContext statistics
    ) {
        this.bodyContext = this.prepareBodyContext(context.withLocalVariables(context.getLocalVariables().resetForProgram()), statistics);
        if (this.body != null && !statistics.getMonitor().isCanceled()) {
            this.body.resolveStatement(this.bodyContext, statistics);
        }
    }

    @Override
    public boolean tryResolveValueRelations(
        @NotNull SQLQueryRowsDataContext context, @NotNull SQLQueryRecognitionContext statistics
    ) {
        return this.body == null || this.bodyContext == null ||
            this.body.tryResolveValueRelations(this.bodyContext.makeEmptyTuple(), statistics);
    }
}
