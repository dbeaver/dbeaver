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
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsSourceModel;
import org.jkiss.dbeaver.model.struct.DBSEntity;

/**
 * Represents any source typically introduced with real table reference in parent context
 */
public class SQLQueryTableRowsContext extends SQLQuerySyntaxContext {
    private final DBSEntity table;
    private final SQLQueryRowsSourceModel source;

    public SQLQueryTableRowsContext(
        @NotNull SQLQueryDataContext parent,
        @NotNull DBSEntity table,
        @NotNull SQLQueryRowsSourceModel source
    ) {
        super(parent);
        this.table = table;
        this.source = source;
    }

    @Nullable
    @Override
    public SQLQueryRowsSourceModel findRealSource(@NotNull DBSEntity table) {
        return this.table.equals(table) ? this.source : super.findRealSource(table);
    }
    
    @Override
    protected void collectKnownSourcesImpl(@NotNull KnownSourcesInfo info) {
        super.collectKnownSourcesImpl(info);
        info.registerTableReference(this.source, this.table);
    }
}