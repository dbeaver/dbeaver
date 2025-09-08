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
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryModelRecognizer;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsSourceContext;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryModelContent;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.stm.STMKnownRuleNames;
import org.jkiss.dbeaver.model.stm.STMTreeNode;
import org.jkiss.dbeaver.model.struct.DBSObjectType;

import java.util.Objects;
import java.util.Set;

public class SQLQueryObjectDropModel extends SQLQueryModelContent {

    @Nullable
    private final SQLQueryObjectDataModel object;
    private final boolean ifExists;

    @NotNull
    public static SQLQueryModelContent recognize(
        @NotNull SQLQueryModelRecognizer recognizer,
        @NotNull STMTreeNode node,
        @NotNull DBSObjectType objectType,
        @NotNull Set<DBSObjectType> objectContainerTypes
    ) {
        SQLQueryObjectDataModel procedure = node.findChildrenOfName(STMKnownRuleNames.qualifiedName).stream()
            .map(recognizer::collectQualifiedName)
            .filter(Objects::nonNull)
            .map(n -> new SQLQueryObjectDataModel(n.syntaxNode, n, objectType, objectContainerTypes))
            .findFirst().orElse(null);
        boolean ifExists = node.findFirstChildOfName(STMKnownRuleNames.ifExistsSpec) != null; // "IF EXISTS" presented
        return new SQLQueryObjectDropModel(node, procedure, ifExists);
    }

    private SQLQueryObjectDropModel(
        @NotNull STMTreeNode syntaxNode,
        @Nullable SQLQueryObjectDataModel object,
        boolean ifExists
    ) {
        super(syntaxNode.getRealInterval(), syntaxNode, object);
        this.object = object;
        this.ifExists = ifExists;
    }

    @Nullable
    public SQLQueryObjectDataModel getObject() {
        return object;
    }

    public boolean getIfExists() {
        return this.ifExists;
    }

    @Override
    public void resolveObjectAndRowsReferences(@NotNull SQLQueryRowsSourceContext context, @NotNull SQLQueryRecognitionContext statistics) {
    }

    @Override
    public void resolveValueRelations(@NotNull SQLQueryRowsDataContext context, @NotNull SQLQueryRecognitionContext statistics) {
    }

    @Nullable
    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
        return visitor.visitObjectStatementDrop(this, arg);
    }

}
