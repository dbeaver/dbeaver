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
package org.jkiss.dbeaver.ui.editors.sql.semantics.context;

import org.antlr.v4.runtime.misc.Interval;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SQLQueryResultTupleContext.SQLQueryResultColumn;
import org.jkiss.dbeaver.ui.editors.sql.semantics.model.SQLQueryRowsSourceModel;

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

    @NotNull
    @Override
    public DBSEntity findRealTable(@NotNull List<String> tableName) {
        return this.parent.findRealTable(tableName);
    }

    @NotNull
    @Override
    public SQLQueryRowsSourceModel findRealSource(@NotNull DBSEntity table) {
        return this.parent.findRealSource(table);
    }

    @Nullable
    @Override
    public SQLQueryResultColumn resolveColumn(@NotNull String columnName) {
        return this.parent.resolveColumn(columnName);
    }

    @NotNull
    @Override
    public SourceResolutionResult resolveSource(@NotNull List<String> tableName) {
        SourceResolutionResult result = super.resolveSource(tableName);
        return result != null ? result : this.parent.resolveSource(tableName);
    }

    @NotNull
    @Override
    public SQLDialect getDialect() {
        return this.parent.getDialect();
    }
    
    @NotNull
    @Override
    public SQLQueryRowsSourceModel getDefaultTable(@NotNull Interval range) {
        return this.parent.getDefaultTable(range);
    }
}


