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
package org.jkiss.dbeaver.ext.clickhouse.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;

import java.sql.Types;

public class ClickhouseArrayType extends ClickhouseAbstractDataType {
    private final DBSDataType componentType;
    private final String name;

    public ClickhouseArrayType(@NotNull ClickhouseDataSource dataSource, @NotNull DBSDataType componentType) {
        super(dataSource);
        this.componentType = componentType;
        this.name = "Array(" + componentType.getFullTypeName() + ")";
    }

    @NotNull
    @Override
    public String getTypeName() {
        return name;
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
