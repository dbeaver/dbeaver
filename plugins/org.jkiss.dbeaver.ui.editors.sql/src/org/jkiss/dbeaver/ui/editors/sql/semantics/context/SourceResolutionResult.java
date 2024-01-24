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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLQuerySymbol;
import org.jkiss.dbeaver.ui.editors.sql.semantics.model.SQLQueryRowsSourceModel;

public class SourceResolutionResult {
    public final SQLQueryRowsSourceModel source;
    public final DBSEntity tableOrNull;
    public final SQLQuerySymbol aliasOrNull;

    private SourceResolutionResult(
        @NotNull SQLQueryRowsSourceModel source,
        @Nullable DBSEntity tableOrNull,
        @Nullable SQLQuerySymbol aliasOrNull
    ) {
        this.source = source;
        this.tableOrNull = tableOrNull;
        this.aliasOrNull = aliasOrNull;
    }

    @NotNull
    public static SourceResolutionResult forRealTableByName(@NotNull SQLQueryRowsSourceModel source, @Nullable DBSEntity table) {
        return new SourceResolutionResult(source, table, null);
    }

    @NotNull
    public static SourceResolutionResult forSourceByAlias(@NotNull SQLQueryRowsSourceModel source, @Nullable SQLQuerySymbol alias) {
        return new SourceResolutionResult(source, null, alias);
    }
}
