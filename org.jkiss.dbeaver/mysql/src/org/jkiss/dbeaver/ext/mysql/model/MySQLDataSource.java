package org.jkiss.dbeaver.ext.mysql.model;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.meta.AbstractDataSource;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.DBPProgressMonitor;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * GenericDataSource
 */
public class MySQLDataSource extends AbstractDataSource implements DBSStructureAssistant,DBSStructureContainerActive
{
    static Log log = LogFactory.getLog(MySQLDataSource.class);

    private List<MySQLCatalog> catalogs;

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

    public void initialize(DBPProgressMonitor monitor)
        throws DBException
    {
        super.initialize(monitor);

        // Read catalogs
        List<MySQLCatalog> tmpCatalogs = new ArrayList<MySQLCatalog>();
        try {
            PreparedStatement dbStat = getConnection().prepareStatement("SELECT * FROM " + MySQLConstants.META_TABLE_SCHEMATA);
            try {
                ResultSet dbResult = dbStat.executeQuery();
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

    public void refreshDataSource(DBPProgressMonitor monitor)
        throws DBException
    {
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

    public boolean refreshObject()
        throws DBException
    {
        return false;
    }

    MySQLTable findTable(String catalogName, String tableName)
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
        return catalog.getTable(tableName);
    }

    public Collection<? extends DBSObject> getChildren()
        throws DBException
    {
        return getCatalogs();
    }

    public DBSObject getChild(String childName)
        throws DBException
    {
        return getCatalog(childName);
    }

    public void cacheStructure()
        throws DBException
    {
        
    }

    public boolean supportsActiveChildChange()
    {
        return true;
    }

    public DBSObject getActiveChild()
        throws DBException
    {
        String activeDbName;
        try {
            PreparedStatement dbStat = getConnection().prepareStatement("select database()");
            try {
                ResultSet resultSet = dbStat.executeQuery();
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
        return getCatalog(activeDbName);
    }

    public void setActiveChild(DBSObject child)
        throws DBException
    {
        if (!(child instanceof MySQLCatalog)) {
            throw new IllegalArgumentException("child");
        }
        try {
            PreparedStatement dbStat = getConnection().prepareStatement("use " + child.getName());
            try {
                dbStat.execute();
            } finally {
                dbStat.close();
            }
        } catch (SQLException e) {
            throw new DBException(e);
        }
    }

    public List<DBSTablePath> findTableNames(String tableMask, int maxResults)
        throws DBException
    {
        List<DBSTablePath> pathList = new ArrayList<DBSTablePath>();
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
                dbResult.close();
            }
            return pathList;
        }
        catch (SQLException ex) {
            throw new DBException(ex);
        }
    }
}
