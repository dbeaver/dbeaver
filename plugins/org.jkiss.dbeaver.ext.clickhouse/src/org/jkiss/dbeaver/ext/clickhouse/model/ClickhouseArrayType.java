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
package org.jkiss.dbeaver.ext.clickhouse.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;

import java.sql.Types;

public class ClickhouseArrayType extends ClickhouseAbstractDataType {
    private final DBSDataType componentType;
    private final String baseName;
    private final String fullName;

    public ClickhouseArrayType(@NotNull ClickhouseDataSource dataSource, @NotNull DBSDataType componentType) {
        this(dataSource, componentType, "Array", "Array(" + componentType.getFullTypeName() + ")");
    }

    protected ClickhouseArrayType(
        @NotNull ClickhouseDataSource dataSource,
        @NotNull DBSDataType componentType,
        @NotNull String baseName,
        @NotNull String fullName
    ) {
        super(dataSource);
        this.componentType = componentType;
        this.baseName = baseName;
        this.fullName = fullName;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof ClickhouseArrayType other && this.componentType.equals(other.componentType);
    }

    @NotNull
    @Override
    public String getTypeName() {
        return baseName;
    }

    @NotNull
    @Override
    public String getFullTypeName() {
        return fullName;
    }

    @Override
    public int getTypeID() {
        return Types.ARRAY;
    }

    @NotNull
    @Override
    public DBPDataKind getDataKind() {
        return DBPDataKind.ARRAY;
    }

    @Nullable
    @Override
    public DBSDataType getComponentType(@NotNull DBRProgressMonitor monitor) {
        return componentType;
    }

    @NotNull
    public DBSDataType getComponentType() {
        return componentType;
    }
}
