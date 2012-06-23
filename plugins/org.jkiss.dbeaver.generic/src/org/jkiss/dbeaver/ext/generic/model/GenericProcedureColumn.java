/*
 *
 *  * Copyright (C) 2010-2012 Serge Rieder
 *  * serge@jkiss.org
 *  *
 *  * This library is free software; you can redistribute it and/or
 *  * modify it under the terms of the GNU Lesser General Public
 *  * License as published by the Free Software Foundation; either
 *  * version 2.1 of the License, or (at your option) any later version.
 *  *
 *  * This library is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  * Lesser General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Lesser General Public
 *  * License along with this library; if not, write to the Free Software
 *  * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCColumn;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSProcedureColumn;
import org.jkiss.dbeaver.model.struct.DBSProcedureColumnType;

/**
 * GenericTable
 */
public class GenericProcedureColumn extends JDBCColumn implements DBSProcedureColumn
{
    private String remarks;
    private GenericProcedure procedure;
    private DBSProcedureColumnType columnType;

    public GenericProcedureColumn(
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
        DBSProcedureColumnType columnType)
    {
        super(columnName,
            typeName,
            valueType,
            ordinalPosition,
            columnSize,
            scale,
            precision,
            notNull);
        this.remarks = remarks;
        this.procedure = procedure;
        this.columnType = columnType;
    }

    @Override
    public DBSObject getParentObject()
    {
        return getProcedure();
    }

    @Override
    public GenericDataSource getDataSource()
    {
        return procedure.getDataSource();
    }

    @Override
    public GenericProcedure getProcedure()
    {
        return procedure;
    }

    @Override
    @Property(name = "Column Type", viewable = true, order = 10)
    public DBSProcedureColumnType getColumnType()
    {
        return columnType;
    }

    @Override
    public String getDescription()
    {
        return remarks;
    }
}
