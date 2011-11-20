/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc;

import org.jkiss.dbeaver.model.DBPClientHome;

import java.io.File;

/**
 * JDBCClientHome
 */
public abstract class JDBCClientHome implements DBPClientHome
{
    private final String id;
    private final File path;

    protected JDBCClientHome(String id, String path)
    {
        this.id = id;
        this.path = new File(path);
    }

    public String getHomeId()
    {
        return id;
    }

    public File getHomePath()
    {
        return path;
    }

}
