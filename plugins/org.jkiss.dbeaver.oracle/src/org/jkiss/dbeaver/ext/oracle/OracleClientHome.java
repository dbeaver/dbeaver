package org.jkiss.dbeaver.ext.oracle;

import org.jkiss.dbeaver.model.impl.jdbc.JDBCClientHome;

/**
 * Oracle client home
 */
public class OracleClientHome extends JDBCClientHome
{
    protected OracleClientHome(String id, String path)
    {
        super(id, path);
    }

    public String getDisplayName()
    {
        return getHomePath();
    }

    public String getProductName()
    {
        return null;
    }

    public String getProductVersion()
    {
        return null;
    }
}
