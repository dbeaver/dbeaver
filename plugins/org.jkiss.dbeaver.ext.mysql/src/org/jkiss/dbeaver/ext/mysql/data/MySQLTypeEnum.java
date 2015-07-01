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
package org.jkiss.dbeaver.ext.mysql.data;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTableColumn;
import org.jkiss.dbeaver.model.data.DBDValue;
import org.jkiss.utils.CommonUtils;

/**
 * Enum type
 */
public class MySQLTypeEnum implements DBDValue {

    private MySQLTableColumn column;
    private String value;

    public MySQLTypeEnum(MySQLTypeEnum source)
    {
        this.column = source.column;
        this.value = source.value;
    }

    public MySQLTypeEnum(MySQLTableColumn column, @Nullable String value)
    {
        this.column = column;
        this.value = value;
    }

    public MySQLTableColumn getColumn()
    {
        return column;
    }

    public String getValue()
    {
        return value;
    }

    @Override
    public Object getRawValue() {
        return value;
    }

    @Override
    public boolean isNull()
    {
        return value == null;
    }

    @Override
    public void release()
    {
        // do nothing
    }

    @Override
    public int hashCode()
    {
        return value == null ? super.hashCode() : value.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        return this == obj ||
            (obj instanceof MySQLTypeEnum && CommonUtils.equalObjects(((MySQLTypeEnum)obj).getValue(), value));
    }
}
