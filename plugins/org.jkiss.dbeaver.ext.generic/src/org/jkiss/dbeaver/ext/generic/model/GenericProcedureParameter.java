/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCAttribute;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameter;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameterKind;

/**
 * GenericTable
 */
public class GenericProcedureParameter extends JDBCAttribute implements DBSProcedureParameter
{
    private String remarks;
    private GenericProcedure procedure;
    private DBSProcedureParameterKind parameterKind;

    public GenericProcedureParameter(
            GenericProcedure procedure,
            String columnName,
            String typeName,
            int valueType,
            int ordinalPosition,
            int columnSize,
            int scale,
            int precision,
            boolean notNull,
            String remarks,
            DBSProcedureParameterKind parameterKind)
    {
        super(columnName,
            typeName,
            valueType,
            ordinalPosition,
            columnSize,
            scale,
            precision,
            notNull,
            false);
        this.remarks = remarks;
        this.procedure = procedure;
        this.parameterKind = parameterKind;
    }

    @NotNull
    @Override
    public GenericDataSource getDataSource()
    {
        return procedure.getDataSource();
    }

    @Override
    public GenericProcedure getParentObject()
    {
        return procedure;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 10)
    public DBSProcedureParameterKind getParameterKind()
    {
        return parameterKind;
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return remarks;
    }

    @NotNull
    @Override
    public DBSTypedObject getParameterType() {
        return this;
    }
}
