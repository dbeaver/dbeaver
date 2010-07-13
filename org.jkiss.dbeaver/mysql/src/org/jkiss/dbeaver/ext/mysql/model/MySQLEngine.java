package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.anno.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.DBException;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * MySQLEngine
 */
public class MySQLEngine implements DBSObject {

    private MySQLDataSource dataSource;
    private String name;
    private String description;
    private boolean isDefault;
    private boolean supported;
    private boolean supportsTransactions;
    private boolean supportsXA;
    private boolean supportsSavepoints;

    public MySQLEngine(MySQLDataSource dataSource, ResultSet dbResult)
        throws SQLException
    {
        this.dataSource = dataSource;
        this.loadInfo(dbResult);
    }

    public MySQLEngine(MySQLDataSource dataSource, String name) {
        this.dataSource = dataSource;
        this.name = name;
    }

    private void loadInfo(ResultSet dbResult)
        throws SQLException
    {
        this.name = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_ENGINE_NAME);
        this.description = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_ENGINE_DESCRIPTION);
        String support = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_ENGINE_SUPPORTED);
        this.isDefault = "DEFAULT".equals(support);
        this.supported = !"NO".equals(support);
        this.supportsTransactions = !"NO".equals(JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_ENGINE_SUPPORT_TXN));
        this.supportsXA = !"NO".equals(JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_ENGINE_SUPPORT_XA));
        this.supportsSavepoints = !"NO".equals(JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_ENGINE_SUPPORT_SAVEPOINTS));
    }

    @Property(name = "Name", viewable = true, order = 1)
    public String getName()
    {
        return name;
    }

    @Property(name = "Description", viewable = true, order = 100)
    public String getDescription()
    {
        return description;
    }

    @Property(name = "Default", viewable = true, order = 2)
    public boolean isDefault()
    {
        return isDefault;
    }

    @Property(name = "Supported", viewable = true, order = 3)
    public boolean isSupported()
    {
        return supported;
    }

    @Property(name = "Supports Transactions", viewable = true, order = 4)
    public boolean isSupportsTransactions()
    {
        return supportsTransactions;
    }

    @Property(name = "Supports XA", viewable = true, order = 5)
    public boolean isSupportsXA()
    {
        return supportsXA;
    }

    @Property(name = "Supports Savepoints", viewable = true, order = 6)
    public boolean isSupportsSavepoints()
    {
        return supportsSavepoints;
    }

    public DBSObject getParentObject()
    {
        return dataSource;
    }

    public DBPDataSource getDataSource()
    {
        return dataSource;
    }

    public boolean refreshObject(DBRProgressMonitor monitor)
        throws DBException
    {
        return false;
    }
}
