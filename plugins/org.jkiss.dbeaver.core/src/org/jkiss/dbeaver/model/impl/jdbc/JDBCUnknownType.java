/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc;

/**
 * JDBCUnknownType
 *
 * @author Serge Rieder
 */
public class JDBCUnknownType {

    private int type;
    private Object value;

    public JDBCUnknownType(int type, Object value)
    {
        this.type = type;
        this.value = value;
    }

    public int getType()
    {
        return type;
    }

    public Object getValue()
    {
        return value;
    }

    public String toString()
    {
        return "Unsupported JDBC type: " + type;
    }

}
