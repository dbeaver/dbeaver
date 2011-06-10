/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * OracleParameter
 */
public class OracleParameter implements DBSObject
{
    static final Log log = LogFactory.getLog(OracleParameter.class);

    private final OracleDataSource dataSource;
    private final String name;
    private Object value;
    private String description;

    public OracleParameter(OracleDataSource dataSource, String name, Object value)
    {
        this.dataSource = dataSource;
        this.name = name;
        this.value = value;
    }

    @Property(name = "Name", viewable = true, order = 1)
    public String getName()
    {
        return name;
    }

    @Property(name = "Value", viewable = true, order = 2)
    public Object getValue()
    {
        return value;
    }

//    @Property(name = "Description", viewable = true, order = 100)
    public String getDescription()
    {
        return description;
    }

    public DBSObject getParentObject()
    {
        return getDataSource();
    }

    public OracleDataSource getDataSource()
    {
        return dataSource;
    }

    public boolean isPersisted()
    {
        return true;
    }
}