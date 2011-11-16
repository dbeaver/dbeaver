package org.jkiss.dbeaver.model.impl.jdbc;

import org.jkiss.dbeaver.model.DBPClientHome;

/**
 * JDBCClientHome
 */
public abstract class JDBCClientHome implements DBPClientHome
{
    private final String id;
    private final String path;

    protected JDBCClientHome(String id, String path)
    {
        this.id = id;
        this.path = path;
    }

    public String getHomeId()
    {
        return id;
    }

    public String getHomePath()
    {
        return path;
    }

}
