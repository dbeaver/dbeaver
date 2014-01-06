/*
 * Copyright (C) 2010-2013 Serge Rieder
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
package org.jkiss.dbeaver.model.impl.jdbc.struct;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * JDBCDataType
 */
public class JDBCDataType implements DBSDataType
{
    private final DBSObject owner;
    private int valueType;
    private String name;
    private String remarks;
    private boolean isUnsigned;
    private boolean isSearchable;
    private int precision;
    private int minScale;
    private int maxScale;

    public JDBCDataType(
        DBSObject owner,
        int valueType,
        String name,
        String remarks,
        boolean unsigned,
        boolean searchable,
        int precision,
        int minScale,
        int maxScale)
    {
        this.owner = owner;
        this.valueType = valueType;
        this.name = name;
        this.remarks = remarks;
        isUnsigned = unsigned;
        isSearchable = searchable;
        this.precision = precision;
        this.minScale = minScale;
        this.maxScale = maxScale;
    }

    @Override
    public String getTypeName()
    {
        return name;
    }

    @Override
    public int getTypeID()
    {
        return valueType;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public String getDescription()
    {
        return remarks;
    }

    @Override
    public DBSObject getParentObject()
    {
        return owner;
    }

    @NotNull
    @Override
    public DBPDataSource getDataSource()
    {
        return owner.getDataSource();
    }

    @Override
    public DBPDataKind getDataKind()
    {
        return JDBCUtils.resolveDataKind(getDataSource(), name, valueType);
    }

    @Override
    public int getScale()
    {
        return minScale;
    }

    public boolean isUnsigned()
    {
        return isUnsigned;
    }

    public boolean isSearchable()
    {
        return isSearchable;
    }

    @Override
    public int getPrecision()
    {
        return precision;
    }

    @Override
    public long getMaxLength()
    {
        return precision;
    }

    @Override
    public int getMinScale()
    {
        return minScale;
    }

    @Override
    public int getMaxScale()
    {
        return maxScale;
    }
    
    public String toString()
    {
        return name;
    }

    @Override
    public boolean isPersisted()
    {
        return true;
    }

}
