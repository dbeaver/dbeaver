/*
 * Copyright (C) 2010-2013 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.mssql.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IAdaptable;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mssql.MSSQLConstants;
import org.jkiss.dbeaver.ext.mssql.MSSQLDataSourceProvider;
import org.jkiss.dbeaver.ext.mssql.model.plan.MSSQLPlanAnalyser;
import org.jkiss.dbeaver.ext.mssql.model.session.MSSQLSessionManager;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManager;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.exec.plan.DBCPlan;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCBasicDataTypeCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * GenericDataSource
 */
public class MSSQLDataSource extends JDBCDataSource implements DBSObjectSelector, DBCQueryPlanner, IAdaptable
{
    static final Log log = LogFactory.getLog(MSSQLDataSource.class);

    private final JDBCBasicDataTypeCache dataTypeCache;
    private List<MSSQLEngine> engines;
    private final CatalogCache catalogCache = new CatalogCache();
    private List<MSSQLPrivilege> privileges;
    private List<MSSQLUser> users;
    private List<MSSQLCharset> charsets;
    private Map<String, MSSQLCollation> collations;
    private String activeCatalogName;
    //private List<MSSQLInformationFolder> informationFolders;

    public MSSQLDataSource(DBRProgressMonitor monitor, DBSDataSourceContainer container)
        throws DBException
    {
        super(monitor, container);
        dataTypeCache = new JDBCBasicDataTypeCache(container);
    }

    @Override
    protected Map<String, String> getInternalConnectionProperties()
    {
        return MSSQLDataSourceProvider.getConnectionsProps();
    }

    public String[] getTableTypes()
    {
        return MSSQLConstants.TABLE_TYPES;
    }

    public CatalogCache getCatalogCache()
    {
        return catalogCache;
    }

    public Collection<MSSQLCatalog> getCatalogs()
    {
        return catalogCache.getCachedObjects();
    }

    public MSSQLCatalog getCatalog(String name)
    {
        return catalogCache.getCachedObject(name);
    }

    @Override
    public void initialize(DBRProgressMonitor monitor)
        throws DBException
    {
        super.initialize(monitor);

        dataTypeCache.getObjects(monitor, this);
        JDBCSession session = openSession(monitor, DBCExecutionPurpose.META, "Load basic datasource metadata");
        try {
            // Read engines
            {
                engines = new ArrayList<MSSQLEngine>();
                JDBCPreparedStatement dbStat = session.prepareStatement("SHOW ENGINES");
                try {
                    JDBCResultSet dbResult = dbStat.executeQuery();
                    try {
                        while (dbResult.next()) {
                            MSSQLEngine engine = new MSSQLEngine(this, dbResult);
                            engines.add(engine);
                        }
                    } finally {
                        dbResult.close();
                    }
                } catch (SQLException ex ) {
                    // Engines are not supported. Shame on it. Leave this list empty
                } finally {
                    dbStat.close();
                }
            }

            // Read charsets and collations
            {
                charsets = new ArrayList<MSSQLCharset>();
                JDBCPreparedStatement dbStat = session.prepareStatement("SHOW CHARSET");
                try {
                    JDBCResultSet dbResult = dbStat.executeQuery();
                    try {
                        while (dbResult.next()) {
                            MSSQLCharset charset = new MSSQLCharset(this, dbResult);
                            charsets.add(charset);
                        }
                    } finally {
                        dbResult.close();
                    }
                } catch (SQLException ex ) {
                    // Engines are not supported. Shame on it. Leave this list empty
                } finally {
                    dbStat.close();
                }
                Collections.sort(charsets, DBUtils.<MSSQLCharset>nameComparator());


                collations = new LinkedHashMap<String, MSSQLCollation>();
                dbStat = session.prepareStatement("SHOW COLLATION");
                try {
                    JDBCResultSet dbResult = dbStat.executeQuery();
                    try {
                        while (dbResult.next()) {
                            String charsetName = JDBCUtils.safeGetString(dbResult, MSSQLConstants.COL_CHARSET);
                            MSSQLCharset charset = getCharset(charsetName);
                            if (charset == null) {
                                log.warn("Charset '" + charsetName + "' not found.");
                                continue;
                            }
                            MSSQLCollation collation = new MSSQLCollation(charset, dbResult);
                            collations.put(collation.getName(), collation);
                            charset.addCollation(collation);
                        }
                    } finally {
                        dbResult.close();
                    }
                } catch (SQLException ex ) {
                    // Engines are not supported. Shame on it. Leave this list empty
                } finally {
                    dbStat.close();
                }
            }

            // Read catalogs
            catalogCache.getObjects(monitor, this);

            {
                // Get active schema
                try {
                    JDBCPreparedStatement dbStat = session.prepareStatement("SELECT DATABASE()");
                    try {
                        JDBCResultSet resultSet = dbStat.executeQuery();
                        try {
                            resultSet.next();
                            activeCatalogName = resultSet.getString(1);
                        } finally {
                            resultSet.close();
                        }
                    } finally {
                        dbStat.close();
                    }
                } catch (SQLException e) {
                    log.error(e);
                }
            }

        } catch (SQLException ex) {
            throw new DBException("Error reading metadata", ex, this);
        }
        finally {
            session.close();
        }
    }

    @Override
    public boolean refreshObject(DBRProgressMonitor monitor)
        throws DBException
    {
        super.refreshObject(monitor);

        this.engines = null;
        this.catalogCache.clearCache();
        this.users = null;
        this.activeCatalogName = null;

        this.initialize(monitor);

        return true;
    }

    MSSQLTable findTable(DBRProgressMonitor monitor, String catalogName, String tableName)
        throws DBException
    {
        if (CommonUtils.isEmpty(catalogName)) {
            return null;
        }
        MSSQLCatalog catalog = getCatalog(catalogName);
        if (catalog == null) {
            log.error("Catalog " + catalogName + " not found");
            return null;
        }
        return catalog.getTable(monitor, tableName);
    }

    @Override
    public Collection<? extends MSSQLCatalog> getChildren(DBRProgressMonitor monitor)
        throws DBException
    {
        return getCatalogs();
    }

    @Override
    public MSSQLCatalog getChild(DBRProgressMonitor monitor, String childName)
        throws DBException
    {
        return getCatalog(childName);
    }

    @Override
    public Class<? extends MSSQLCatalog> getChildType(DBRProgressMonitor monitor)
        throws DBException
    {
        return MSSQLCatalog.class;
    }

    @Override
    public void cacheStructure(DBRProgressMonitor monitor, int scope)
        throws DBException
    {
        
    }

    @Override
    public boolean supportsObjectSelect()
    {
        return true;
    }

    @Override
    public MSSQLCatalog getSelectedObject()
    {
        return getCatalog(activeCatalogName);
    }

    @Override
    public void selectObject(DBRProgressMonitor monitor, DBSObject object)
        throws DBException
    {
        final MSSQLCatalog oldSelectedEntity = getSelectedObject();
        if (!(object instanceof MSSQLCatalog)) {
            throw new IllegalArgumentException("Invalid object type: " + object);
        }
        JDBCSession session = openSession(monitor, DBCExecutionPurpose.UTIL, "Set active catalog");
        try {
            JDBCPreparedStatement dbStat = session.prepareStatement("use " + DBUtils.getQuotedIdentifier(object));
            try {
                dbStat.execute();
            } finally {
                dbStat.close();
            }
        } catch (SQLException e) {
            throw new DBException(e, this);
        }
        finally {
            session.close();
        }
        activeCatalogName = object.getName();

        // Send notifications
        if (oldSelectedEntity != null) {
            DBUtils.fireObjectSelect(oldSelectedEntity, false);
        }
        if (this.activeCatalogName != null) {
            DBUtils.fireObjectSelect(object, true);
        }
    }

    public List<MSSQLUser> getUsers(DBRProgressMonitor monitor)
        throws DBException
    {
        if (users == null) {
            users = loadUsers(monitor);
        }
        return users;
    }

    public MSSQLUser getUser(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return DBUtils.findObject(getUsers(monitor), name);
    }

    private List<MSSQLUser> loadUsers(DBRProgressMonitor monitor)
        throws DBException
    {
        JDBCSession session = getDataSource().openSession(monitor, DBCExecutionPurpose.META, "Load users");
        try {
            JDBCPreparedStatement dbStat = session.prepareStatement("SELECT * FROM mssql.user ORDER BY user");
            try {
                JDBCResultSet dbResult = dbStat.executeQuery();
                try {
                    List<MSSQLUser> userList = new ArrayList<MSSQLUser>();
                    while (dbResult.next()) {
                            MSSQLUser user = new MSSQLUser(this, dbResult);
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
            throw new DBException(ex, this);
        }
        finally {
            session.close();
        }
    }

    public List<MSSQLEngine> getEngines()
    {
        return engines;
    }

    public MSSQLEngine getEngine(String name)
    {
        return DBUtils.findObject(engines, name);
    }

    public MSSQLEngine getDefaultEngine()
    {
        for (MSSQLEngine engine : engines) {
            if (engine.getSupport() == MSSQLEngine.Support.DEFAULT) {
                return engine;
            }
        }
        return null;
    }

    public Collection<MSSQLCharset> getCharsets()
    {
        return charsets;
    }

    public MSSQLCharset getCharset(String name)
    {
        for (MSSQLCharset charset : charsets) {
            if (charset.getName().equals(name)) {
                return charset;
            }
        }
        return null;
    }

    public MSSQLCollation getCollation(String name)
    {
        return collations.get(name);
    }

    public List<MSSQLPrivilege> getPrivileges(DBRProgressMonitor monitor)
        throws DBException
    {
        if (privileges == null) {
            privileges = loadPrivileges(monitor);
        }
        return privileges;
    }

    public List<MSSQLPrivilege> getPrivilegesByKind(DBRProgressMonitor monitor, MSSQLPrivilege.Kind kind)
        throws DBException
    {
        List<MSSQLPrivilege> privs = new ArrayList<MSSQLPrivilege>();
        for (MSSQLPrivilege priv : getPrivileges(monitor)) {
            if (priv.getKind() == kind) {
                privs.add(priv);
            }
        }
        return privs;
    }

    public MSSQLPrivilege getPrivilege(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return DBUtils.findObject(getPrivileges(monitor), name);
    }

    private List<MSSQLPrivilege> loadPrivileges(DBRProgressMonitor monitor)
        throws DBException
    {
        JDBCSession session = openSession(monitor, DBCExecutionPurpose.META, "Load privileges");
        try {
            JDBCPreparedStatement dbStat = session.prepareStatement("SHOW PRIVILEGES");
            try {
                JDBCResultSet dbResult = dbStat.executeQuery();
                try {
                    List<MSSQLPrivilege> privileges = new ArrayList<MSSQLPrivilege>();
                    while (dbResult.next()) {
                            MSSQLPrivilege user = new MSSQLPrivilege(this, dbResult);
                            privileges.add(user);
                        }
                    return privileges;
                } finally {
                    dbResult.close();
                }
            } finally {
                dbStat.close();
            }
        }
        catch (SQLException ex) {
            throw new DBException(ex, this);
        }
        finally {
            session.close();
        }
    }

    public List<MSSQLParameter> getSessionStatus(DBRProgressMonitor monitor)
        throws DBException
    {
        return loadParameters(monitor, true, false);
    }

    public List<MSSQLParameter> getGlobalStatus(DBRProgressMonitor monitor)
        throws DBException
    {
        return loadParameters(monitor, true, true);
    }

    public List<MSSQLParameter> getSessionVariables(DBRProgressMonitor monitor)
        throws DBException
    {
        return loadParameters(monitor, false, false);
    }

    public List<MSSQLParameter> getGlobalVariables(DBRProgressMonitor monitor)
        throws DBException
    {
        return loadParameters(monitor, false, true);
    }

    public List<MSSQLDataSource> getInformation()
    {
        return Collections.singletonList(this);
    }

    private List<MSSQLParameter> loadParameters(DBRProgressMonitor monitor, boolean status, boolean global) throws DBException
    {
        JDBCSession session = openSession(monitor, DBCExecutionPurpose.META, "Load status");
        try {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SHOW " + 
                (global ? "GLOBAL " : "") + 
                (status ? "STATUS" : "VARIABLES"));
            try {
                JDBCResultSet dbResult = dbStat.executeQuery();
                try {
                    List<MSSQLParameter> parameters = new ArrayList<MSSQLParameter>();
                    while (dbResult.next()) {
                        MSSQLParameter parameter = new MSSQLParameter(
                            this,
                            JDBCUtils.safeGetString(dbResult, "variable_name"),
                            JDBCUtils.safeGetString(dbResult, "value"));
                        parameters.add(parameter);
                    }
                    return parameters;
                } finally {
                    dbResult.close();
                }
            } finally {
                dbStat.close();
            }
        }
        catch (SQLException ex) {
            throw new DBException(ex, this);
        }
        finally {
            session.close();
        }
    }

    @Override
    public DBCPlan planQueryExecution(DBCSession session, String query) throws DBCException
    {
        MSSQLPlanAnalyser plan = new MSSQLPlanAnalyser(this, query);
        plan.explain(session);
        return plan;
    }

    @Override
    public Object getAdapter(Class adapter)
    {
        if (adapter == DBSStructureAssistant.class) {
            return new MSSQLStructureAssistant(this);
        } else if (adapter == DBAServerSessionManager.class) {
            return new MSSQLSessionManager(this);
        }
        return null;
    }

    @NotNull
    @Override
    public MSSQLDataSource getDataSource() {
        return this;
    }

    @Override
    public Collection<? extends DBSDataType> getDataTypes()
    {
        return dataTypeCache.getCachedObjects();
    }

    @Override
    public DBSDataType getDataType(String typeName)
    {
        return dataTypeCache.getCachedObject(typeName);
    }

    static class CatalogCache extends JDBCObjectCache<MSSQLDataSource, MSSQLCatalog>
    {
        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCSession session, MSSQLDataSource owner) throws SQLException
        {
            StringBuilder catalogQuery = new StringBuilder("SELECT * FROM " + MSSQLConstants.META_TABLE_SCHEMATA);
            DBSObjectFilter catalogFilters = owner.getContainer().getObjectFilter(MSSQLCatalog.class, null);
            if (catalogFilters != null) {
                JDBCUtils.appendFilterClause(catalogQuery, catalogFilters, MSSQLConstants.COL_SCHEMA_NAME, true);
            }
            JDBCPreparedStatement dbStat = session.prepareStatement(catalogQuery.toString());
            if (catalogFilters != null) {
                JDBCUtils.setFilterParameters(dbStat, 1, catalogFilters);
            }
            return dbStat;
        }

        @Override
        protected MSSQLCatalog fetchObject(JDBCSession session, MSSQLDataSource owner, ResultSet resultSet) throws SQLException, DBException
        {
            return new MSSQLCatalog(owner, resultSet);
        }

    }

}
