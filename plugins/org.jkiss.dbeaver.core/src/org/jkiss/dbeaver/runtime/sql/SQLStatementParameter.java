/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.sql;

import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

/**
 * SQL statement parameter info
 */
public class SQLStatementParameter {
    private DBDValueHandler valueHandler;
    DBSTypedObject paramType;
    private int index;
    private String name;
    private Object value;

    public SQLStatementParameter(DBDValueHandler valueHandler, DBSTypedObject paramType, int index, String name, Object value)
    {
        this.valueHandler = valueHandler;
        this.paramType = paramType;
        this.index = index;
        this.name = name;
        this.value = value;
    }

    public boolean isResolved()
    {
        return valueHandler != null;
    }

    public DBDValueHandler getValueHandler()
    {
        return valueHandler;
    }

    public DBSTypedObject getParamType()
    {
        return paramType;
    }

    public int getIndex()
    {
        return index;
    }

    public String getName()
    {
        return name;
    }

    public Object getValue()
    {
        return value;
    }

    @Override
    public String toString()
    {
        return valueHandler.getValueDisplayString(paramType, value);
    }
}
