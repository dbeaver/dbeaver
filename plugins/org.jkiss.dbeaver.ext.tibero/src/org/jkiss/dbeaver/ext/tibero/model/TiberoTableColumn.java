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

package org.jkiss.dbeaver.ext.tibero.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableColumn;

public class TiberoTableColumn extends JDBCTableColumn<TiberoTableBase> {

    private final String description;

    public TiberoTableColumn(
        @NotNull TiberoTableBase table,
        @NotNull String name,
        @NotNull String typeName,
        int valueType,
        int ordinalPosition,
        long maxLength,
        @Nullable Integer scale,
        @Nullable Integer precision,
        boolean required,
        @Nullable String defaultValue,
        @Nullable String description
    ) {
        super(table, true, name, typeName, valueType, ordinalPosition, maxLength, scale, precision, required, false, defaultValue);
        this.description = description;
    }

    @NotNull
    @Override
    public TiberoDataSource getDataSource() {
        return getTable().getDataSource();
    }

    @Nullable
    @Override
    public String getDescription() {
        return description;
    }
}
