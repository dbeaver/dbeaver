/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
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

    public boolean isNull()
    {
        return value == null;
    }

    public DBDValue makeNull()
    {
        return new OracleObjectValue(null);
    }

    public void release()
    {

    }
}
