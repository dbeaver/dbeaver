/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.mssql.model;

import org.eclipse.core.runtime.IAdaptable;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.ext.mssql.SQLServerConstants;
import org.jkiss.dbeaver.ext.mssql.SQLServerUtils;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SQLServerDataSource extends JDBCDataSource implements DBSObjectSelector, DBSInstanceContainer, /*DBCQueryPlanner, */IAdaptable {

    private static final Log log = Log.getLog(SQLServerDataSource.class);

    // Delegate data type reading to the driver
    private final SystemDataTypeCache dataTypeCache = new SystemDataTypeCache();
    private final DatabaseCache databaseCache = new DatabaseCache();

    private String activeDatabaseName;

    public SQLServerDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container)
        throws DBException
    {
        super(monitor, container, new SQLServerDialect());
    }

    @Override
    public DBPDataSource getDataSource() {
        return this;
    }

    @Override
    protected Map<String, String> getInternalConnectionProperties(DBRProgressMonitor monitor, DBPDriver driver, String purpose, DBPConnectionConfiguration connectionInfo) throws DBCException {
        Map<String, String> connectionsProps = new HashMap<>();
        if (!getContainer().getPreferenceStore().getBoolean(ModelPreferences.META_CLIENT_NAME_DISABLE)) {
            // App name
            connectionsProps.put(
                SQLServerUtils.isDriverJtds(driver) ? SQLServerConstants.APPNAME_CLIENT_PROPERTY : SQLServerConstants.APPLICATION_NAME_CLIENT_PROPERTY,
                CommonUtils.truncateString(DBUtils.getClientApplicationName(getContainer(), purpose), 64));
        }
        return connectionsProps;
    }

    @Override
    public Object getDataSourceFeature(String featureId) {
        switch (featureId) {
            case DBConstants.FEATURE_LIMIT_AFFECTS_DML:
                return true;
            case DBConstants.FEATURE_MAX_STRING_LENGTH:
                return 8000;
        }
        return super.getDataSourceFeature(featureId);
    }

    @Override
    public void initialize(DBRProgressMonitor monitor) throws DBException {
        super.initialize(monitor);

        this.dataTypeCache.getAllObjects(monitor, this);

        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load data source meta info")) {
            this.activeDatabaseName = SQLServerUtils.getCurrentDatabase(session);
        } catch (Throwable e) {
            log.error("Error during connection initialization", e);
        }
    }

    //////////////////////////////////////////////////////////////////
    // Data types

    @Override
    public DBPDataKind resolveDataKind(String typeName, int valueType) {
        return getLocalDataType(valueType).getDataKind();
    }

    @Override
    public List<SQLServerDataType> getLocalDataTypes() {
        return dataTypeCache.getCachedObjects();
    }

    @Override
    public SQLServerDataType getLocalDataType(String typeName) {
        return dataTypeCache.getCachedObject(typeName);
    }

    @Override
    public SQLServerDataType getLocalDataType(int typeID) {
        DBSDataType dt = super.getLocalDataType(typeID);
        if (dt == null) {
            log.debug("System data type " + typeID + " not found");
            return null;
        }
        return (SQLServerDataType) dt;
    }

    //////////////////////////////////////////////////////////
    // Databases

    protected boolean isShowAllSchemas() {
        return CommonUtils.toBoolean(getContainer().getConnectionConfiguration().getProviderProperty(SQLServerConstants.PROP_SHOW_ALL_SCHEMAS));
    }

    //////////////////////////////////////////////////////////
    // Windows authentication

    @Override
    protected String getConnectionUserName(DBPConnectionConfiguration connectionInfo) {
        if (SQLServerUtils.isWindowsAuth(connectionInfo)) {
            return "";
        } else {
            return super.getConnectionUserName(connectionInfo);
        }
    }

    @Override
    protected String getConnectionUserPassword(DBPConnectionConfiguration connectionInfo) {
        if (SQLServerUtils.isWindowsAuth(connectionInfo)) {
            return "";
        } else {
            return super.getConnectionUserPassword(connectionInfo);
        }
    }

    //////////////////////////////////////////////////////////////
    // Databases

    @Override
    public boolean supportsDefaultChange() {
        return true;
    }

    @Nullable
    @Override
    public SQLServerDatabase getDefaultObject() {
        return activeDatabaseName == null ? null : databaseCache.getCachedObject(activeDatabaseName);
    }

    @Override
    public void setDefaultObject(@NotNull DBRProgressMonitor monitor, @NotNull DBSObject object)
        throws DBException
    {
        final SQLServerDatabase oldSelectedEntity = getDefaultObject();
        if (!(object instanceof SQLServerDatabase)) {
            throw new IllegalArgumentException("Invalid object type: " + object);
        }
        for (JDBCExecutionContext context : getDefaultInstance().getAllContexts()) {
            setCurrentDatabase(monitor, context, (SQLServerDatabase) object);
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

    @Override
    public boolean refreshDefaultObject(@NotNull DBCSession session) throws DBException {
        try {
            final String currentSchema = SQLServerUtils.getCurrentDatabase((JDBCSession) session);
            if (currentSchema != null && !CommonUtils.equalObjects(currentSchema, activeDatabaseName)) {
                final SQLServerDatabase newDatabase = databaseCache.getCachedObject(currentSchema);
                if (newDatabase != null) {
                    setDefaultObject(session.getProgressMonitor(), newDatabase);
                    return true;
                }
            }
            return false;
        } catch (SQLException e) {
            throw new DBException(e, this);
        }
    }

    private void setCurrentDatabase(DBRProgressMonitor monitor, JDBCExecutionContext executionContext, SQLServerDatabase object) throws DBCException {
        if (object == null) {
            log.debug("Null current schema");
            return;
        }
        try (JDBCSession session = executionContext.openSession(monitor, DBCExecutionPurpose.UTIL, "Set active database")) {
            SQLServerUtils.setCurrentDatabase(session, object.getName());
        } catch (SQLException e) {
            throw new DBCException(e, this);
        }
    }

    @Association
    public Collection<SQLServerDatabase> getDatabases(DBRProgressMonitor monitor) throws DBException {
        return databaseCache.getAllObjects(monitor, this);
    }

    @Override
    public Collection<? extends DBSObject> getChildren(DBRProgressMonitor monitor) throws DBException {
        return databaseCache.getAllObjects(monitor, this);
    }

    @Override
    public DBSObject getChild(DBRProgressMonitor monitor, String childName) throws DBException {
        return databaseCache.getObject(monitor, this, childName);
    }

    @Override
    public Class<? extends DBSObject> getChildType(DBRProgressMonitor monitor) throws DBException {
        return SQLServerDatabase.class;
    }

    @Override
    public void cacheStructure(DBRProgressMonitor monitor, int scope) throws DBException {
        databaseCache.getAllObjects(monitor, this);
    }

    @Override
    public DBSObject refreshObject(DBRProgressMonitor monitor) throws DBException {
        databaseCache.clearCache();
        return super.refreshObject(monitor);
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == DBSStructureAssistant.class) {
            return adapter.cast(new SQLServerStructureAssistant(this));
        }
        return super.getAdapter(adapter);
    }

    static class DatabaseCache extends JDBCObjectCache<SQLServerDataSource, SQLServerDatabase> {
        DatabaseCache() {
            setListOrderComparator(DBUtils.nameComparator());
        }

        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull SQLServerDataSource owner) throws SQLException {
            StringBuilder sql = new StringBuilder("SELECT db.* FROM sys.databases db");

            DBSObjectFilter databaseFilters = owner.getContainer().getObjectFilter(SQLServerDatabase.class, null, false);
            if (databaseFilters != null && databaseFilters.isEnabled()) {
                JDBCUtils.appendFilterClause(sql, databaseFilters, "name", true);
            }
            sql.append("\nORDER BY db.name");
            return session.prepareStatement(sql.toString());
        }

        @Override
        protected SQLServerDatabase fetchObject(@NotNull JDBCSession session, @NotNull SQLServerDataSource owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new SQLServerDatabase(owner, resultSet);
        }

    }

    private class SystemDataTypeCache extends JDBCObjectCache<SQLServerDataSource, SQLServerDataType> {
        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCSession session, SQLServerDataSource sqlServerDataSource) throws SQLException {
            return session.prepareStatement("SELECT * FROM sys.types WHERE is_user_defined = 0 order by name");
        }

        @Override
        protected SQLServerDataType fetchObject(JDBCSession session, SQLServerDataSource dataSource, JDBCResultSet resultSet) throws SQLException, DBException {
            return new SQLServerDataType(dataSource, resultSet);
        }
    }
}
