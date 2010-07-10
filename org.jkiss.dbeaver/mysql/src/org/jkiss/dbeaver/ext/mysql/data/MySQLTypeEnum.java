/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.data;

import org.jkiss.dbeaver.model.data.DBDValue;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTableColumn;

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
}
