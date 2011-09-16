/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.sql;

import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

/**
 * SQL statement parameter info
 */
public class SQLStatementParameter implements DBSTypedObject {
    private DBDValueHandler valueHandler;
    DBSDataType paramType;
    private int index;
    private String name;
    private Object value;

    public SQLStatementParameter(DBDValueHandler valueHandler, DBSDataType paramType, int index, String name, Object value)
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

    public void resolve(DBDValueHandler valueHandler, DBSDataType paramType, Object value)
    {
        this.valueHandler = valueHandler;
        this.paramType = paramType;
        this.value = value;
    }

    public DBDValueHandler getValueHandler()
    {
        return valueHandler;
    }

    public DBSDataType getParamType()
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
        return getTitle() + "=" + (isResolved() ? valueHandler.getValueDisplayString(this, value) : "?");
    }

    public String getTitle()
    {
        if (name == null) {
            return String.valueOf(index);
        }
        if (name.startsWith(":")) {
            return name.substring(1);
        } else {
            return name;
        }
    }

    public String getTypeName()
    {
        return paramType == null ? "" : paramType.getName();
    }

    public int getValueType()
    {
        return paramType == null ? -1 : paramType.getValueType();
    }

    public int getScale()
    {
        return paramType == null ? 0 : paramType.getMinScale();
    }

    public int getPrecision()
    {
        return paramType == null ? 0 : paramType.getPrecision();
    }
}
