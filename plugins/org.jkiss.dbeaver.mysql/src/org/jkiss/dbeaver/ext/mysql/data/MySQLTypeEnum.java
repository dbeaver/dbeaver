/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.data;

import net.sf.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTableColumn;
import org.jkiss.dbeaver.model.data.DBDValue;

/**
 * Enum type
 */
public class MySQLTypeEnum implements DBDValue {

    private MySQLTableColumn column;
    private String value;

    public MySQLTypeEnum(MySQLTableColumn column, String value)
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

    public boolean isNull()
    {
        return value == null;
    }

    public DBDValue makeNull()
    {
        return new MySQLTypeEnum(column, null);
    }

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
