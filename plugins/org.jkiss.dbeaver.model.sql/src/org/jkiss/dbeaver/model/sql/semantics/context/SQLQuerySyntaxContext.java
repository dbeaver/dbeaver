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
package org.jkiss.dbeaver.model.sql.semantics.context;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsSourceModel;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectType;

import java.util.List;

/**
 * Represents any derived context based on relational operations in parent context
 */
public abstract class SQLQuerySyntaxContext extends SQLQueryDataContext {
    protected final SQLQueryDataContext parent;

    public SQLQuerySyntaxContext(@NotNull SQLQueryDataContext parent) {
        this.parent = parent;
    }

    @NotNull
    @Override
    public List<SQLQueryResultColumn> getColumnsList() {
        return this.parent.getColumnsList();
    }

    @Override
    public boolean hasUnresolvedSource() {
        return this.parent.hasUnresolvedSource();
    }


    @NotNull
    @Override
    public List<SQLQueryResultPseudoColumn> getPseudoColumnsList() {
        return this.parent.getPseudoColumnsList();
    }

    @Nullable
    @Override
    public DBSEntity findRealTable(@NotNull DBRProgressMonitor monitor, @NotNull List<String> tableName) {
        return this.parent.findRealTable(monitor, tableName);
    }

    @Nullable
    @Override
    public SQLQueryRowsSourceModel findRealSource(@NotNull DBSEntity table) {
        return this.parent.findRealSource(table);
    }

    @Nullable
    @Override
    public DBSObject findRealObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSObjectType objectType,
        @NotNull List<String> objectName
    ) {
        return this.parent.findRealObject(monitor, objectType, objectName);
    }

    @Nullable
    @Override
    public SQLQueryResultColumn resolveColumn(@NotNull DBRProgressMonitor monitor, @NotNull String columnName) {
        return this.parent.resolveColumn(monitor, columnName);
    }

    @Override
    @Nullable
    public SQLQueryResultPseudoColumn resolvePseudoColumn(DBRProgressMonitor monitor, @NotNull String name) {
        return this.parent.resolvePseudoColumn(monitor, name);
    }

    @Nullable
    @Override
    public SQLQueryResultPseudoColumn resolveGlobalPseudoColumn(@NotNull DBRProgressMonitor monitor, @NotNull String name) {
        return this.parent.resolveGlobalPseudoColumn(monitor, name);
    }

    @Nullable
    @Override
    public SourceResolutionResult resolveSource(@NotNull DBRProgressMonitor monitor, @NotNull List<String> tableName) {
        SourceResolutionResult result = super.resolveSource(monitor, tableName);
        return result != null ? result : this.parent.resolveSource(monitor, tableName);
    }

    @NotNull
    @Override
    public SQLDialect getDialect() {
        return this.parent.getDialect();
    }
    
    @Override
    protected void collectKnownSourcesImpl(@NotNull KnownSourcesInfo result) {
        this.parent.collectKnownSourcesImpl(result);
    }

    @Override
    protected final List<SQLQueryResultPseudoColumn> prepareRowsetPseudoColumns(@NotNull SQLQueryRowsSourceModel source) {
        return this.parent.prepareRowsetPseudoColumns(source);
    }
}


