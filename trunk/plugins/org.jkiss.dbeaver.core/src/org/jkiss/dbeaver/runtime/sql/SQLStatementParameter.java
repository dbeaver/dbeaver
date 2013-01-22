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
package org.jkiss.dbeaver.runtime.sql;

import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataKind;
import org.jkiss.dbeaver.model.struct.DBSDataType;

/**
 * SQL statement parameter info
 */
public class SQLStatementParameter implements DBSAttributeBase {
    private DBDValueHandler valueHandler;
    private DBSDataType paramType;
    private int index;
    private String name;
    private Object value;

    public SQLStatementParameter(int index, String name)
    {
        this.index = index;
        this.name = name;
    }

    public boolean isResolved()
    {
        return valueHandler != null;
    }

    public void resolve()
    {
        if (paramType == null) {
            return;
        }
        this.valueHandler = DBUtils.findValueHandler(
            paramType.getDataSource(),
            paramType.getDataSource().getContainer(),
            paramType.getName(),
            paramType.getTypeID());
    }

    public DBDValueHandler getValueHandler()
    {
        return valueHandler;
    }

    public DBSDataType getParamType()
    {
        return paramType;
    }

    public void setParamType(DBSDataType paramType)
    {
        this.paramType = paramType;
    }

    public int getIndex()
    {
        return index;
    }

    @Override
    public String getName()
    {
        return name;
    }

    public boolean isNotNull()
    {
        return false;
    }

    @Override
    public long getMaxLength()
    {
        return 0;
    }

    public Object getValue()
    {
        return value;
    }

    public void setValue(Object value)
    {
        this.value = value;
    }

    @Override
    public String toString()
    {
        return getTitle() + "=" + (isResolved() ? valueHandler.getValueDisplayString(this, value) : "?");
    }

    public String getTitle()
    {
        if (name.startsWith(":")) {
            return name.substring(1);
        } else {
            return name;
        }
    }

    @Override
    public String getTypeName()
    {
        return paramType == null ? "" : paramType.getName();
    }

    @Override
    public int getTypeID()
    {
        return paramType == null ? -1 : paramType.getTypeID();
    }

    @Override
    public DBSDataKind getDataKind()
    {
        return paramType.getDataKind();
    }

    @Override
    public int getScale()
    {
        return paramType == null ? 0 : paramType.getScale();
    }

    @Override
    public int getPrecision()
    {
        return paramType == null ? 0 : paramType.getPrecision();
    }

    @Override
    public boolean isRequired()
    {
        return false;
    }
}
