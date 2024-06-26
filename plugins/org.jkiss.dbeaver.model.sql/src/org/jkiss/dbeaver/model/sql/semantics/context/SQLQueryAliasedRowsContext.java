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
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbol;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsSourceModel;

import java.util.List;

/**
 * Represents aliased source introduced with table correlation in parent context
 */
public class SQLQueryAliasedRowsContext extends SQLQuerySyntaxContext {
    private final SQLQuerySymbol alias;
    private final SQLQueryRowsSourceModel source;

    public SQLQueryAliasedRowsContext(
        @NotNull SQLQueryDataContext parent,
        @NotNull SQLQuerySymbol alias,
        @NotNull SQLQueryRowsSourceModel source
    ) {
        super(parent);
        this.alias = alias;
        this.source = source;
    }

    @Nullable
    @Override
    public SourceResolutionResult resolveSource(@NotNull DBRProgressMonitor monitor, @NotNull List<String> tableName) {
        return tableName.size() == 1 && tableName.get(0).equals(this.alias.getName())
            ? SourceResolutionResult.forSourceByAlias(this.source, this.alias)
            : super.resolveSource(monitor, tableName);
    }
    
    @Override
    protected void collectKnownSourcesImpl(@NotNull KnownSourcesInfo info) {
        super.collectKnownSourcesImpl(info);
        info.registerAlias(this.source, this.alias);
    }
}

