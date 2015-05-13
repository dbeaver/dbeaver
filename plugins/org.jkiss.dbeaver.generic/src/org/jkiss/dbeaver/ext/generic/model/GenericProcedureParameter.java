/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCColumn;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameter;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameterType;

/**
 * GenericTable
 */
public class GenericProcedureParameter extends JDBCColumn implements DBSProcedureParameter
{
    private String remarks;
    private GenericProcedure procedure;
    private DBSProcedureParameterType parameterType;

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
            DBSProcedureParameterType parameterType)
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
        this.parameterType = parameterType;
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

    @Override
    @Property(viewable = true, order = 10)
    public DBSProcedureParameterType getParameterType()
    {
        return parameterType;
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return remarks;
    }
}
