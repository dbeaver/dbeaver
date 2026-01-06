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
package org.jkiss.dbeaver.ext.mssql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

public class SQLServerTypedObject implements DBSTypedObject {
    private String typeName;
    private int typeId;
    private DBPDataKind dataKind;
    private int scale;
    private int precision;
    private int maxLength;

    public SQLServerTypedObject(String typeName, int typeId, DBPDataKind dataKind, int scale, int precision, int maxLength) {
        this.typeName = typeName;
        this.typeId = typeId;
        this.dataKind = dataKind;
        this.scale = scale;
        this.precision = precision;
        this.maxLength = maxLength;
    }

    @NotNull
    @Override
    public String getTypeName() {
        return typeName;
    }

    @NotNull
    @Override
    public String getFullTypeName() {
        return typeName;
    }

    @Override
    public int getTypeID() {
        return typeId;
    }

    @NotNull
    @Override
    public DBPDataKind getDataKind() {
        return dataKind;
    }

    @Nullable
    @Override
    public Integer getScale() {
        return scale;
    }

    @Nullable
    @Override
    public Integer getPrecision() {
        return precision;
    }

    @Override
    public long getMaxLength() {
        return maxLength;
    }

    @Override
    public long getTypeModifiers() {
        return 0;
    }
}
