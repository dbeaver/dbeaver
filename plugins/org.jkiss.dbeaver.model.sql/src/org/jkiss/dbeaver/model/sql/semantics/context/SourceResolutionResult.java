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
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbol;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsCteSubqueryModel;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsSourceModel;
import org.jkiss.dbeaver.model.struct.DBSEntity;

/**
 * Describes the result of the query source resolution
 */
public class SourceResolutionResult {
    @NotNull
    public final SQLQueryRowsSourceModel source;
    @Nullable
    public final DBSEntity tableOrNull;
    @Nullable
    public final SQLQuerySymbol aliasOrNull;
    public final boolean isCteSubquery;

    private SourceResolutionResult(
        @NotNull SQLQueryRowsSourceModel source,
        @Nullable DBSEntity tableOrNull,
        @Nullable SQLQuerySymbol aliasOrNull
    ) {
        this.source = source;
        this.tableOrNull = tableOrNull;
        this.aliasOrNull = aliasOrNull;
        this.isCteSubquery = source instanceof SQLQueryRowsCteSubqueryModel;
    }

    /**
     * Builds a new instance for a table by metadata without alias
     */
    @NotNull
    public static SourceResolutionResult forRealTableByName(@NotNull SQLQueryRowsSourceModel source, @Nullable DBSEntity table) {
        return new SourceResolutionResult(source, table, null);
    }

    /**
     * Builds a new instance for a table by its alias
     */
    @NotNull
    public static SourceResolutionResult forSourceByAlias(@NotNull SQLQueryRowsSourceModel source, @Nullable SQLQuerySymbol alias) {
        return new SourceResolutionResult(source, null, alias);
    }

    /**
     * Builds a new instance for a table from metadata with alias
     */
    @NotNull
    public static SourceResolutionResult withRealTable(@NotNull SourceResolutionResult rr, @Nullable DBSEntity table) {
        return new SourceResolutionResult(rr.source, table, rr.aliasOrNull);
    }

    /**
     * Builds a new instance for a table by its name and alias
     */
    @NotNull
    public static SourceResolutionResult withAlias(@NotNull SourceResolutionResult rr, @Nullable SQLQuerySymbol alias) {
        return new SourceResolutionResult(rr.source, rr.tableOrNull, alias);
    }
}
