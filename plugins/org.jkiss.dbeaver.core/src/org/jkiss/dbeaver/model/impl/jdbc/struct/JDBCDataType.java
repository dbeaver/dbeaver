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
package org.jkiss.dbeaver.model.impl.jdbc.struct;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.struct.DBSDataKind;
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
    public int getValueType()
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

    @Override
    public DBPDataSource getDataSource()
    {
        return owner.getDataSource();
    }

    @Override
    public DBSDataKind getDataKind()
    {
        return getDataKind(valueType);
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

    public static DBSDataKind getDataKind(int valueType)
    {
        switch (valueType) {
            case java.sql.Types.BOOLEAN:
                return DBSDataKind.BOOLEAN;
            case java.sql.Types.CHAR:
            case java.sql.Types.VARCHAR:
            case java.sql.Types.LONGVARCHAR:
                return DBSDataKind.STRING;
            case java.sql.Types.BIGINT:
            case java.sql.Types.BIT:
            case java.sql.Types.DECIMAL:
            case java.sql.Types.DOUBLE:
            case java.sql.Types.FLOAT:
            case java.sql.Types.INTEGER:
            case java.sql.Types.NUMERIC:
            case java.sql.Types.REAL:
            case java.sql.Types.SMALLINT:
            case java.sql.Types.TINYINT:
                return DBSDataKind.NUMERIC;
            case java.sql.Types.DATE:
            case java.sql.Types.TIME:
            case java.sql.Types.TIMESTAMP:
                return DBSDataKind.DATETIME;
            case java.sql.Types.BLOB:
            case java.sql.Types.CLOB:
            case java.sql.Types.VARBINARY:
            case java.sql.Types.LONGVARBINARY:
                return DBSDataKind.LOB;
            case java.sql.Types.STRUCT:
                return DBSDataKind.STRUCT;
            case java.sql.Types.ARRAY:
                return DBSDataKind.ARRAY;
        }
        return DBSDataKind.UNKNOWN;
    }

}
