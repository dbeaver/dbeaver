/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.access.DBAPrivilege;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;

/**
 * MySQLParameter
 */
public class MySQLParameter implements DBSObject
{
    static final Log log = LogFactory.getLog(MySQLParameter.class);

    private final MySQLDataSource dataSource;
    private final String name;
    private Object value;
    private String description;

    public MySQLParameter(MySQLDataSource dataSource, String name, Object value)
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

    public String getObjectId()
    {
        return getName();
    }

    @Property(name = "Description", viewable = true, order = 100)
    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public DBSObject getParentObject()
    {
        return getDataSource();
    }

    public MySQLDataSource getDataSource()
    {
        return dataSource;
    }
}