/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCDataType;
import org.jkiss.dbeaver.model.struct.DBSDataKind;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.DataTypeProviderDescriptor;

/**
 * Array holder
 */
public class JDBCArrayType implements DBSTypedObject {
    private String typeName;
    private int typeID;
    private DBDValueHandler valueHandler;

    public JDBCArrayType(String typeName, int typeID)
    {
        this.typeName = typeName;
        this.typeID = typeID;
    }

    @Override
    public String getTypeName()
    {
        return typeName;
    }

    @Override
    public int getTypeID()
    {
        return typeID;
    }

    @Override
    public DBSDataKind getDataKind()
    {
        return JDBCDataType.getDataKind(typeName, typeID);
    }

    @Override
    public int getScale()
    {
        return 0;
    }

    @Override
    public int getPrecision()
    {
        return 0;
    }

    @Override
    public long getMaxLength()
    {
        return 0;
    }

    public DBDValueHandler getValueHandler()
    {
        return valueHandler;
    }

    void resolveHandler(DBCExecutionContext context)
    {
        valueHandler = DBUtils.findValueHandler(context.getDataSource(), context, typeName, typeID);
    }

}