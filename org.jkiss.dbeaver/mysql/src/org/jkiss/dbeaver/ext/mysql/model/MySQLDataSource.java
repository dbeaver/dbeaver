package org.jkiss.dbeaver.ext.mysql.model;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.jdbc.JDBCResultSet;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * GenericDataSource
 */
public class MySQLDataSource extends JDBCDataSource implements DBSStructureAssistant,DBSStructureContainerActive
{
    static Log log = LogFactory.getLog(MySQLDataSource.class);

    private List<MySQLCatalog> catalogs;
    private MySQLCatalog activeCatalog;

    public MySQLDataSource(DBSDataSourceContainer container)
        throws DBException
    {
        super(container);
    }

    public String[] getTableTypes()
    {
        return MySQLConstants.TABLE_TYPES;
    }

    public List<MySQLCatalog> getCatalogs()
    {
        return catalogs;
    }

    public MySQLCatalog getCatalog(String name)
    {
        return DBSUtils.findObject(catalogs, name);
    }

    public void initialize(DBRProgressMonitor monitor)
        throws DBException
    {
        super.initialize(monitor);

        // Read catalogs
        List<MySQLCatalog> tmpCatalogs = new ArrayList<MySQLCatalog>();
        try {
            JDBCPreparedStatement dbStat = getExecutionContext(monitor).prepareStatement("SELECT * FROM " + MySQLConstants.META_TABLE_SCHEMATA);
            try {
                JDBCResultSet dbResult = dbStat.executeQuery();
                try {
                    while (dbResult.next()) {
                        String schemaName = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_SCHEMA_NAME);
                        if (!CommonUtils.isEmpty(schemaName)) {
                            MySQLCatalog catalog = new MySQLCatalog(this, schemaName);
                            catalog.setDefaultCharset(
                                JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_DEFAULT_CHARACTER_SET_NAME));
                            catalog.setDefaultCollation(
                                JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_DEFAULT_COLLATION_NAME));
                            catalog.setSqlPath(
                                JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_SQL_PATH));
                            tmpCatalogs.add(catalog);
                        }
                    }
                } finally {
                    dbResult.close();
                }
            }
            finally {
                dbStat.close();
            }
        } catch (SQLException ex) {
            throw new DBException("Error reading metadata", ex);
        }
        this.catalogs = tmpCatalogs;
    }

    public void refreshDataSource(DBRProgressMonitor monitor)
        throws DBException
    {
        this.activeCatalog = null;
        this.catalogs = null;

        this.initialize(monitor);
    }

    public void close()
    {
        super.close();
    }

    public void cancelCurrentOperation()
    {
        try {
            reconnect();
        } catch (Exception ex) {
            log.error(ex);
        }
    }

    public boolean refreshObject(DBRProgressMonitor monitor)
        throws DBException
    {
        return false;
    }

    MySQLTable findTable(DBRProgressMonitor monitor, String catalogName, String tableName)
        throws DBException
    {
        if (CommonUtils.isEmpty(catalogName)) {
            return null;
        }
        MySQLCatalog catalog = getCatalog(catalogName);
        if (catalog == null) {
            log.error("Catalog " + catalogName + " not found");
            return null;
        }
        return catalog.getTable(monitor, tableName);
    }

    public Collection<? extends DBSObject> getChildren(DBRProgressMonitor monitor)
        throws DBException
    {
        return getCatalogs();
    }

    public DBSObject getChild(DBRProgressMonitor monitor, String childName)
        throws DBException
    {
        return getCatalog(childName);
    }

    public void cacheStructure(DBRProgressMonitor monitor, int scope)
        throws DBException
    {
        
    }

    public boolean supportsActiveChildChange()
    {
        return true;
    }

    public DBSObject getActiveChild(DBRProgressMonitor monitor)
        throws DBException
    {
        if (this.activeCatalog == null) {
            String activeDbName;
            try {
                JDBCPreparedStatement dbStat = getExecutionContext(monitor).prepareStatement("select database()");
                try {
                    JDBCResultSet resultSet = dbStat.executeQuery();
                    try {
                        resultSet.next();
                        activeDbName = resultSet.getString(1);
                    } finally {
                        resultSet.close();
                    }
                } finally {
                    dbStat.close();
                }
            } catch (SQLException e) {
                log.error(e);
                return null;
            }
            this.activeCatalog = getCatalog(activeDbName);
        }
        return this.activeCatalog;
    }

    public void setActiveChild(DBRProgressMonitor monitor, DBSObject child)
        throws DBException
    {
        if (child == activeCatalog) {
            return;
        }
        if (!(child instanceof MySQLCatalog)) {
            throw new IllegalArgumentException("child");
        }
        try {
            JDBCPreparedStatement dbStat = getExecutionContext(monitor).prepareStatement("use " + child.getName());
            try {
                dbStat.execute();
            } finally {
                dbStat.close();
            }
        } catch (SQLException e) {
            throw new DBException(e);
        }
        
        // Send notifications
        DBSObject oldChild = this.activeCatalog;
        this.activeCatalog = (MySQLCatalog) child;

        if (oldChild != null) {
            getContainer().fireEvent(DBSObjectAction.CHANGED, oldChild);
        }
        if (this.activeCatalog != null) {
            getContainer().fireEvent(DBSObjectAction.CHANGED, this.activeCatalog);
        }
    }

    public List<DBSTablePath> findTableNames(DBRProgressMonitor monitor, String tableMask, int maxResults)
        throws DBException
    {
        List<DBSTablePath> pathList = new ArrayList<DBSTablePath>();
        JDBCUtils.startConnectionBlock(monitor, this, "Find table names");
        try {
            DatabaseMetaData metaData = getDataSource().getConnection().getMetaData();

            // Make table mask uppercase
            tableMask = tableMask.toUpperCase();

            // Load tables
            ResultSet dbResult = metaData.getTables(
                null,
                null,
                tableMask,
                null);
            try {
                int tableNum = maxResults;
                while (dbResult.next() && tableNum-- > 0) {

                    String catalogName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TABLE_CAT);
                    String tableName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TABLE_NAME);
                    String tableType = JDBCUtils.safeGetString(dbResult, JDBCConstants.TABLE_TYPE);
                    String remarks = JDBCUtils.safeGetString(dbResult, JDBCConstants.REMARKS);

                    pathList.add(
                        new DBSTablePath(
                            catalogName,
                            null,
                            tableName,
                            tableType,
                            remarks));
                }
            }
            finally {
                JDBCUtils.safeClose(dbResult);
            }
            return pathList;
        }
        catch (SQLException ex) {
            throw new DBException(ex);
        }
        finally {
            JDBCUtils.endConnectionBlock(monitor);
        }
    }
}
