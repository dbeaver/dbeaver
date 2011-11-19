/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCClientHome;

/**
 * MySQLServerHome
 */
public class MySQLServerHome extends JDBCClientHome {

    private String name;

    protected MySQLServerHome(String path, String name)
    {
        super(path, path);
        this.name = name == null ? path : name;
    }

    public String getDisplayName()
    {
        return name;
    }

    public String getProductName() throws DBException
    {
        return "MySQL";
    }

    public String getProductVersion() throws DBException
    {
        return "1";
    }
}
