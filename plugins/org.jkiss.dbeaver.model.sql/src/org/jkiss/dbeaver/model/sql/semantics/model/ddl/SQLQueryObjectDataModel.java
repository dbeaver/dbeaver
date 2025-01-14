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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.sql.semantics.*;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsSourceModel;
import org.jkiss.dbeaver.model.stm.STMTreeNode;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectType;

import java.util.List;


/**
 * Describes object reference
 * @apiNote
 * TODO remove objectType and treat this as non-table rows source like table-producing procedures, no matter builtin or not
 *      (see something like {@code SELECT * FROM proc()}  )
 */
public class SQLQueryObjectDataModel extends SQLQueryRowsSourceModel implements SQLQuerySymbolDefinition {

    private static final Log log = Log.getLog(SQLQueryObjectDataModel.class);
    @NotNull
    private final SQLQueryQualifiedName name;
    @NotNull
    private DBSObjectType objectType;
    @Nullable
    private DBSObject object = null;

    public SQLQueryObjectDataModel(
        @NotNull STMTreeNode syntaxNode,
        @NotNull SQLQueryQualifiedName name,
        @NotNull DBSObjectType objectType) {
        super(syntaxNode);
        this.name = name;
        this.objectType = objectType;
    }

    @NotNull
    public DBSObjectType getObjectType() {
        return objectType;
    }

    @NotNull
    public SQLQueryQualifiedName getName() {
        return this.name;
    }

    @Nullable
    public DBSObject getObject() {
        return object;
    }

    @NotNull
    @Override
    public SQLQuerySymbolClass getSymbolClass() {
        return this.object != null ? SQLQuerySymbolClass.TABLE : SQLQuerySymbolClass.ERROR;
    }

    @NotNull
    @Override
    protected SQLQueryDataContext propagateContextImpl(@NotNull SQLQueryDataContext context, @NotNull SQLQueryRecognitionContext statistics) {
        if (this.name.isNotClassified()) {
            List<String> nameStrings = this.name.toListOfStrings();
            this.object = context.findRealObject(statistics.getMonitor(), objectType, nameStrings);

            if (this.object != null) {
                this.name.setDefinition(object, new SQLQuerySymbolOrigin.DbObjectFromContext(context));
            } else {
                statistics.appendError(getSyntaxNode(), "Object " + this.name.toIdentifierString() + " not found in the database");
            }
        }
        return context;
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
        return visitor.visitObjectReference(this, arg);
    }
}