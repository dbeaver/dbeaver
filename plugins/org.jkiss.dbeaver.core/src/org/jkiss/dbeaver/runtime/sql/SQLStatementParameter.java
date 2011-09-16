/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.sql;

import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.struct.DBSColumnBase;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

/**
 * SQL statement parameter info
 */
public class SQLStatementParameter implements DBSColumnBase {
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
            paramType.getValueType());
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

    public String getName()
    {
        return name;
    }

    public boolean isNotNull()
    {
        return false;
    }

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
