/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.model;

import com.mysql.jdbc.ConnectionImpl;
import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.ext.mysql.MySQLDataSourceProvider;
import org.jkiss.dbeaver.ext.mysql.model.plan.MySQLPlanAnalyser;
import org.jkiss.dbeaver.model.DBPEvent;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformType;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformer;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.exec.plan.DBCPlan;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

/**
 * GenericDataSource
 */
public class MySQLDataSource extends JDBCDataSource implements DBSStructureAssistant, DBSEntitySelector, DBCQueryPlanner
{
    static final Log log = LogFactory.getLog(MySQLDataSource.class);

    private List<MySQLEngine> engines;
    private List<MySQLCatalog> catalogs;
    private List<MySQLUser> users;
    private MySQLCatalog activeCatalog;

    public MySQLDataSource(DBSDataSourceContainer container)
        throws DBException
    {
        super(container);
    }

    protected Properties getInternalConnectionProperties()
    {
        return MySQLDataSourceProvider.getConnectionsProps();
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
        return DBUtils.findObject(catalogs, name);
    }

    public List<MySQLEngine> getEngines()
    {
        return engines;
    }

    public MySQLEngine getEngine(String name)
    {
        return DBUtils.findObject(engines, name);
    }

    public void initialize(DBRProgressMonitor monitor)
        throws DBException
    {
        super.initialize(monitor);

        JDBCExecutionContext context = openContext(monitor);
        try {
            // Read engines
            List<MySQLEngine> tmpEngines = new ArrayList<MySQLEngine>();
            JDBCPreparedStatement dbStat = context.prepareStatement("SHOW ENGINES");
            try {
                JDBCResultSet dbResult = dbStat.executeQuery();
                try {
                    while (dbResult.next()) {
                        MySQLEngine engine = new MySQLEngine(this, dbResult);
                        tmpEngines.add(engine);
                    }
                } finally {
                    dbResult.close();
                }
            } catch (SQLException ex ) {
                // Engines are not supported. Shame on it. Leave this list empty
            } finally {
                dbStat.close();
            }
            this.engines = tmpEngines;

            // Read catalogs
            List<MySQLCatalog> tmpCatalogs = new ArrayList<MySQLCatalog>();
            dbStat = context.prepareStatement("SELECT * FROM " + MySQLConstants.META_TABLE_SCHEMATA);
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
            this.catalogs = tmpCatalogs;
        } catch (SQLException ex) {
            throw new DBException("Error reading metadata", ex);
        }
        finally {
            context.close();
        }
    }

    public boolean refreshEntity(DBRProgressMonitor monitor)
        throws DBException
    {
        super.refreshEntity(monitor);

        this.engines = null;
        this.catalogs = null;
        this.users = null;
        this.activeCatalog = null;

        this.initialize(monitor);

        return true;
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

    public Collection<? extends DBSEntity> getChildren(DBRProgressMonitor monitor)
        throws DBException
    {
        return getCatalogs();
    }

    public DBSEntity getChild(DBRProgressMonitor monitor, String childName)
        throws DBException
    {
        return getCatalog(childName);
    }

    public Class<? extends DBSEntity> getChildType(DBRProgressMonitor monitor)
        throws DBException
    {
        return MySQLCatalog.class;
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
            JDBCExecutionContext context = openContext(monitor);
            try {
                JDBCPreparedStatement dbStat = context.prepareStatement("select database()");
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
            finally {
                context.close();
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
            throw new IllegalArgumentException("Invalid active object type");
        }
        JDBCExecutionContext context = openContext(monitor);
        try {
            JDBCPreparedStatement dbStat = context.prepareStatement("use " + child.getName());
            try {
                dbStat.execute();
            } finally {
                dbStat.close();
            }
        } catch (SQLException e) {
            throw new DBException(e);
        }
        finally {
            context.close();
        }
        
        // Send notifications
        DBSObject oldChild = this.activeCatalog;
        this.activeCatalog = (MySQLCatalog) child;

        if (oldChild != null) {
            getContainer().fireEvent(new DBPEvent(DBPEvent.Action.OBJECT_UPDATE, oldChild));
        }
        if (this.activeCatalog != null) {
            getContainer().fireEvent(new DBPEvent(DBPEvent.Action.OBJECT_UPDATE, this.activeCatalog));
        }
    }

    public List<DBSTablePath> findTableNames(DBRProgressMonitor monitor, String tableMask, int maxResults)
        throws DBException
    {
        List<DBSTablePath> pathList = new ArrayList<DBSTablePath>();
        JDBCUtils.startConnectionBlock(monitor, this, "Find table names");
        JDBCExecutionContext context = getDataSource().openContext(monitor);
        try {
            // Load tables
            JDBCPreparedStatement dbStat = context.prepareStatement(
                "SELECT TABLE_SCHEMA,TABLE_NAME,TABLE_TYPE,TABLE_COMMENT FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME LIKE '" + tableMask.toLowerCase() + "%'");
            try {
                JDBCResultSet dbResult = dbStat.executeQuery();
                try {
                    int tableNum = maxResults;
                    while (dbResult.next() && tableNum-- > 0) {
    
                        String catalogName = JDBCUtils.safeGetString(dbResult, 1);
                        String tableName = JDBCUtils.safeGetString(dbResult, 2);
                        String tableType = JDBCUtils.safeGetString(dbResult, 3);
                        String remarks = JDBCUtils.safeGetString(dbResult, 4);

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
            } finally {
                dbStat.close();
            }
            return pathList;
        }
        catch (SQLException ex) {
            throw new DBException(ex);
        }
        finally {
            context.close();
            JDBCUtils.endConnectionBlock(monitor);
        }
    }

    @Override
    protected Connection openConnection() throws DBException {
        ConnectionImpl mysqlConnection = (ConnectionImpl)super.openConnection();

        // Fix "errorMessageEncoding" error. Dirty hack.
        // characterSetMetadata -> errorMessageEncoding
        try {
            Field characterSetMetadataField = ConnectionImpl.class.getDeclaredField("characterSetMetadata");
            Field errorMessageEncodingField = ConnectionImpl.class.getDeclaredField("errorMessageEncoding");
            characterSetMetadataField.setAccessible(true);
            errorMessageEncodingField.setAccessible(true);
            errorMessageEncodingField.set(
                mysqlConnection,
                characterSetMetadataField.get(mysqlConnection));
        } catch (Throwable e) {
            log.debug(e);
        }

        return mysqlConnection;
    }


    public List<MySQLUser> getUsers(DBRProgressMonitor monitor)
        throws DBException
    {
        if (users == null) {
            users = loadUsers(monitor);
        }
        return users;
    }

    public MySQLUser getUser(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return DBUtils.findObject(getUsers(monitor), name);
    }

    private List<MySQLUser> loadUsers(DBRProgressMonitor monitor)
        throws DBException
    {
        JDBCExecutionContext context = getDataSource().openContext(monitor, "Load users");
        try {
            JDBCPreparedStatement dbStat = context.prepareStatement("SELECT * FROM mysql.user ORDER BY user");
            try {
                JDBCResultSet dbResult = dbStat.executeQuery();
                try {
                    List<MySQLUser> userList = new ArrayList<MySQLUser>();
                    while (dbResult.next()) {
                            MySQLUser user = new MySQLUser(this, dbResult);
                            userList.add(user);
                        }
                    return userList;
                } finally {
                    dbResult.close();
                }
            } finally {
                dbStat.close();
            }
        }
        catch (SQLException ex) {
            throw new DBException(ex);
        }
        finally {
            context.close();
        }
    }

    @Override
    public DBCQueryTransformer createQueryTransformer(DBCQueryTransformType type) {
        if (type == DBCQueryTransformType.RESULT_SET_LIMIT) {
            return new QueryTransformerLimit();
        } else if (type == DBCQueryTransformType.FETCH_ALL_TABLE) {
            return new QueryTransformerFetchAll();
        }
        return super.createQueryTransformer(type);
    }

    public DBCPlan planQueryExecution(DBCExecutionContext context, String query) throws DBCException
    {
        MySQLPlanAnalyser plan = new MySQLPlanAnalyser(this, query);
        plan.explain(context);
        return plan;
    }

}
