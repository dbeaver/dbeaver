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
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsSourceModel;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;

public class SQLQueryResultColumn {
    public final int index;
    @NotNull
    public final SQLQuerySymbol symbol;
    @NotNull
    public final SQLQueryRowsSourceModel source;
    @Nullable
    public final DBSEntity realSource;
    @Nullable
    public final DBSEntityAttribute realAttr;
    @NotNull
    public final SQLQueryExprType type;

    public SQLQueryResultColumn(
        int index,
        @NotNull SQLQuerySymbol symbol,
        @NotNull SQLQueryRowsSourceModel source,
        @Nullable DBSEntity realSource,
        @Nullable DBSEntityAttribute realAttr,
        @NotNull SQLQueryExprType type
    ) {
        this.index = index;
        this.symbol = symbol;
        this.source = source;
        this.realSource = realSource;
        this.realAttr = realAttr;
        this.type = type;
    }

    public SQLQueryResultColumn withNewIndex(int index) {
        return new SQLQueryResultColumn(index, this.symbol, this.source, this.realSource, this.realAttr, this.type);
    }
}

