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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameter;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameterKind;

/**
 * PostgreProcedureParameter
 */
public class PostgreProcedureParameter implements DBSProcedureParameter, DBSAttributeBase, DBSObject
{
    private PostgreProcedure procedure;
    private String paramName;
    private int ordinalPosition;
    private PostgreDataType dataType;
    private PostgreProcedure.ArgumentMode argumentMode;
    private String defaultValue;

    public PostgreProcedureParameter(
        PostgreProcedure procedure,
        String paramName,
        PostgreDataType dataType,
        @NotNull PostgreProcedure.ArgumentMode argumentMode,
        int ordinalPosition)
    {
        this.procedure = procedure;
        this.paramName = paramName;
        this.dataType = dataType;
        this.argumentMode = argumentMode;
        this.ordinalPosition = ordinalPosition;
    }

    @NotNull
    @Override
    public PostgreDataSource getDataSource()
    {
        return procedure.getDataSource();
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public PostgreProcedure getParentObject()
    {
        return procedure;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return paramName;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 2)
    public PostgreDataType getParameterType() {
        return dataType;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 3)
    public DBSProcedureParameterKind getParameterKind()
    {
        return argumentMode.getParameterKind();
    }

    @Override
    @Property(viewable = true, order = 4)
    public int getOrdinalPosition() {
        return ordinalPosition;
    }

    @Override
    public boolean isRequired() {
        return false;
    }

    @Override
    public boolean isAutoGenerated() {
        return false;
    }

    @NotNull
    @Override
    public String getTypeName() {
        return dataType.getTypeName();
    }

    @NotNull
    @Override
    public String getFullTypeName() {
        return dataType.getFullTypeName();
    }

    @Override
    public int getTypeID() {
        return dataType.getTypeID();
    }

    @NotNull
    @Override
    public DBPDataKind getDataKind() {
        return dataType.getDataKind();
    }

    @Override
    public Integer getScale() {
        return dataType.getScale();
    }

    @Nullable
    @Override
    public Integer getPrecision() {
        return dataType.getPrecision();
    }

    @Override
    public long getMaxLength() {
        return dataType.getMaxLength();
    }

    @Override
    public long getTypeModifiers() {
        return 0;
    }

    @NotNull
    public PostgreProcedure.ArgumentMode getArgumentMode() {
        return argumentMode;
    }

    @Property(viewable = true, order = 5)
    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }
}
