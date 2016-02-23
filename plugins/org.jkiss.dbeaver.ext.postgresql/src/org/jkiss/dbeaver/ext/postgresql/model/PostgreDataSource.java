/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.postgresql.model;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.PostgreDataSourceProvider;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.ext.postgresql.model.jdbc.PostgreJdbcFactory;
import org.jkiss.dbeaver.ext.postgresql.model.plan.PostgrePlanAnalyser;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPErrorAssistant;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.*;
import org.jkiss.dbeaver.model.exec.plan.DBCPlan;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.utils.CommonUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PostgreGenericDataSource
 */
public class PostgreDataSource extends JDBCDataSource implements DBSObjectSelector, DBSInstanceContainer, DBCQueryPlanner, IAdaptable
{

    static final Log log = Log.getLog(PostgreDataSource.class);

    private final DatabaseCache databaseCache = new DatabaseCache();
    private String activeDatabaseName;
    private final List<String> searchPath = new ArrayList<>();
    private String activeUser;

    public PostgreDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container)
        throws DBException
    {
        super(monitor, container);
    }

    @Override
    protected Map<String, String> getInternalConnectionProperties()
    {
        return PostgreDataSourceProvider.getConnectionsProps();
    }

    protected void initializeContextState(@NotNull DBRProgressMonitor monitor, @NotNull JDBCExecutionContext context, boolean setActiveObject) throws DBCException {
        if (setActiveObject) {
            PostgreDatabase object = getSelectedObject();
            if (object != null) {
                useDatabase(monitor, context, object);
            }
        }
    }

    @Override
    protected PostgreDialect createSQLDialect(@NotNull JDBCDatabaseMetaData metaData) {
        return new PostgreDialect(this, metaData);
    }

    public DatabaseCache getDatabaseCache()
    {
        return databaseCache;
    }

    public Collection<PostgreDatabase> getDatabases()
    {
        return databaseCache.getCachedObjects();
    }

    public PostgreDatabase getDatabase(String name)
    {
        return databaseCache.getCachedObject(name);
    }

    @Override
    public void initialize(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        super.initialize(monitor);

        activeDatabaseName = getContainer().getConnectionConfiguration().getDatabaseName();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load meta info")) {
            activeUser = JDBCUtils.queryString(session, "SELECT SESSION_USER");
            String searchPathStr = JDBCUtils.queryString(session, "SHOW search_path");
            if (searchPathStr != null) {
                Collections.addAll(this.searchPath, searchPathStr.replace("$user", activeUser).split(","));
            } else {
                this.searchPath.add("public");
            }
        } catch (SQLException e) {
            log.error("Error reading connection meta info");
        }

        // Read databases
        databaseCache.getAllObjects(monitor, this);
        final PostgreDatabase defaultInstance = getDefaultInstance();
        getDefaultInstance().dataTypeCache.getAllObjects(monitor, defaultInstance);
    }

    @Override
    public boolean refreshObject(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        super.refreshObject(monitor);

        this.databaseCache.clearCache();
        this.activeDatabaseName = null;

        this.initialize(monitor);

        return true;
    }

    @Override
    public Collection<? extends PostgreDatabase> getChildren(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return getDatabases();
    }

    @Override
    public PostgreDatabase getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName)
        throws DBException
    {
        return getDatabase(childName);
    }

    @Override
    public Class<? extends PostgreDatabase> getChildType(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return PostgreDatabase.class;
    }

    @Override
    public void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope)
        throws DBException
    {
        databaseCache.getAllObjects(monitor, this);
    }

    @Override
    public boolean supportsObjectSelect()
    {
        return true;
    }

    @Override
    public PostgreDatabase getSelectedObject()
    {
        return getDatabase(activeDatabaseName);
    }

    @Override
    public void selectObject(DBRProgressMonitor monitor, DBSObject object)
        throws DBException
    {
        final PostgreDatabase oldSelectedEntity = getSelectedObject();
        if (!(object instanceof PostgreDatabase)) {
            throw new IllegalArgumentException("Invalid object type: " + object);
        }
        for (JDBCExecutionContext context : getAllContexts()) {
            useDatabase(monitor, context, (PostgreDatabase) object);
        }
        activeDatabaseName = object.getName();

        // Send notifications
        if (oldSelectedEntity != null) {
            DBUtils.fireObjectSelect(oldSelectedEntity, false);
        }
        if (this.activeDatabaseName != null) {
            DBUtils.fireObjectSelect(object, true);
        }
    }

    public String getActiveUser() {
        return activeUser;
    }

    public List<String> getSearchPath() {
        return searchPath;
    }

    public void setSearchPath(String path) {
        searchPath.clear();
        searchPath.add(path);
        if (!path.equals(activeUser)) {
            searchPath.add(activeUser);
        }
    }

    private void useDatabase(DBRProgressMonitor monitor, JDBCExecutionContext context, PostgreDatabase catalog) throws DBCException {
        //throw new DBCException("Active database change not supported yet");
    }

    @Override
    protected Connection openConnection(@NotNull DBRProgressMonitor monitor, @NotNull String purpose) throws DBCException {
        Connection mysqlConnection = super.openConnection(monitor, purpose);

        {
            // Provide client info
            IProduct product = Platform.getProduct();
            if (product != null) {
                String appName = DBeaverCore.getProductTitle();
                try {
                    mysqlConnection.setClientInfo("ApplicationName", appName + " - " + purpose);
                } catch (Throwable e) {
                    // just ignore
                    log.debug(e);
                }
            }
        }

        return mysqlConnection;
    }

    @Override
    public DBCPlan planQueryExecution(DBCSession session, String query) throws DBCException
    {
        PostgrePlanAnalyser plan = new PostgrePlanAnalyser(query);
        plan.explain(session);
        return plan;
    }

    @Override
    public Object getAdapter(Class adapter)
    {
        if (adapter == DBSStructureAssistant.class) {
            return new PostgreStructureAssistant(this);
        }
/*
        else if (adapter == DBAServerSessionManager.class) {
            return new PostgreSessionManager(this);
        }
*/
        return super.getAdapter(adapter);
    }

    @NotNull
    @Override
    public PostgreDataSource getDataSource() {
        return this;
    }

    @Override
    public Collection<PostgreDataType> getLocalDataTypes()
    {
        return getDefaultInstance().dataTypeCache.getCachedObjects();
    }

    @Override
    public PostgreDataType getLocalDataType(String typeName)
    {
        return getDefaultInstance().dataTypeCache.getCachedObject(typeName);
    }

    @Override
    public String getDefaultDataTypeName(@NotNull DBPDataKind dataKind) {
        return PostgreUtils.getDefaultDataTypeName(dataKind);
    }

    @NotNull
    @Override
    public PostgreDatabase getDefaultInstance() {
        PostgreDatabase defDatabase = databaseCache.getCachedObject(activeDatabaseName);
        if (defDatabase == null) {
            defDatabase = databaseCache.getCachedObject(PostgreConstants.DEFAULT_DATABASE);
        }
        if (defDatabase == null) {
            final List<PostgreDatabase> allDatabases = databaseCache.getCachedObjects();
            if (allDatabases.isEmpty()) {
                throw new IllegalStateException("No default database");
            }
            defDatabase = allDatabases.get(0);
        }
        return defDatabase;
    }

    @NotNull
    @Override
    public Collection<PostgreDatabase> getAvailableInstances() {
        return databaseCache.getCachedObjects();
    }

    class DatabaseCache extends JDBCObjectCache<PostgreDataSource, PostgreDatabase>
    {
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull PostgreDataSource owner) throws SQLException
        {
            final boolean hideNDD = CommonUtils.toBoolean(getContainer().getActualConnectionConfiguration().getProperty(PostgreConstants.PROP_HIDE_NON_DEFAULT_DB));
            StringBuilder catalogQuery = new StringBuilder(
                "SELECT db.oid,db.*" +
                "\nFROM pg_catalog.pg_database db WHERE NOT datistemplate AND datallowconn");
            if (hideNDD) {
                catalogQuery.append("\nAND db.datname=?");
            }
            DBSObjectFilter catalogFilters = owner.getContainer().getObjectFilter(PostgreDatabase.class, null, false);
            if (!hideNDD) {
                if (catalogFilters != null) {
                    JDBCUtils.appendFilterClause(catalogQuery, catalogFilters, "datname", true);
                }
                catalogQuery.append("\nORDER BY db.datname");
            }
            JDBCPreparedStatement dbStat = session.prepareStatement(catalogQuery.toString());
            if (hideNDD) {
                dbStat.setString(1, activeDatabaseName);
            } else if (catalogFilters != null) {
                JDBCUtils.setFilterParameters(dbStat, 1, catalogFilters);
            }
            return dbStat;
        }

        @Override
        protected PostgreDatabase fetchObject(@NotNull JDBCSession session, @NotNull PostgreDataSource owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException
        {
            return new PostgreDatabase(owner, resultSet);
        }

    }

    private Pattern ERROR_POSITION_PATTERN = Pattern.compile("\\n\\s*Position: ([0-9]+)");

    @Nullable
    @Override
    public ErrorPosition[] getErrorPosition(@NotNull Throwable error) {
        String message = error.getMessage();
        if (!CommonUtils.isEmpty(message)) {
            Matcher matcher = ERROR_POSITION_PATTERN.matcher(message);
            if (matcher.find()) {
                DBPErrorAssistant.ErrorPosition pos = new DBPErrorAssistant.ErrorPosition();
                pos.position = Integer.parseInt(matcher.group(1)) - 1;
                return new ErrorPosition[] {pos};
            }
        }
        return null;
    }

    @NotNull
    @Override
    protected JDBCFactory createJdbcFactory() {
        return new PostgreJdbcFactory();
    }

}
