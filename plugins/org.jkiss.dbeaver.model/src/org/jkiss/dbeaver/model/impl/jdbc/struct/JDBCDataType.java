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
package org.jkiss.dbeaver.model.impl.jdbc.struct;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

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
        @Nullable String remarks,
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

    public JDBCDataType(DBSObject owner, DBSTypedObject typed) {
        this(owner, typed.getTypeID(), typed.getTypeName(), null, false, false, typed.getPrecision(), typed.getScale(), typed.getScale());
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

    @NotNull
    @Override
    public String getName()
    {
        return name;
    }

    @Nullable
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

    @Nullable
    @Override
    public DBSDataType getComponentType(@NotNull DBRProgressMonitor monitor) throws DBCException {
        return null;
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
