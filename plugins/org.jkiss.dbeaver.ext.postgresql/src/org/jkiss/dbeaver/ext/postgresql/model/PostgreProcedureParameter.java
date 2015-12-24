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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCColumn;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameter;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameterKind;

/**
 * PostgreProcedureParameter
 */
public class PostgreProcedureParameter extends JDBCColumn implements DBSProcedureParameter, DBSTypedObject
{
    private PostgreProcedure procedure;
    private DBSProcedureParameterKind parameterKind;

    public PostgreProcedureParameter(
        PostgreProcedure procedure,
        String columnName,
        String typeName,
        int valueType,
        int ordinalPosition,
        long columnSize,
        int scale,
        int precision,
        boolean notNull,
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
        this.procedure = procedure;
        this.parameterKind = parameterKind;
    }

    @NotNull
    @Override
    public PostgreDataSource getDataSource()
    {
        return procedure.getDataSource();
    }

    @Override
    public PostgreProcedure getParentObject()
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

    @NotNull
    @Override
    public DBSTypedObject getParameterType() {
        return this;
    }
}
