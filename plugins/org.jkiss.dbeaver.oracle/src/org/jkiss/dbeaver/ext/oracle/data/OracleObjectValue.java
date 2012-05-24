/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.data;

import org.jkiss.dbeaver.model.data.DBDValue;

/**
 * Object value
 */
public class OracleObjectValue implements DBDValue{

    private Object value;

    public OracleObjectValue(Object value)
    {
        this.value = value;
    }

    @Override
    public boolean isNull()
    {
        return value == null;
    }

    @Override
    public DBDValue makeNull()
    {
        return new OracleObjectValue(null);
    }

    @Override
    public void release()
    {

    }
}
