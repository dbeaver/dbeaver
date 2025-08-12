/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.snowflake.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericCatalog;
import org.jkiss.dbeaver.ext.generic.model.GenericExecutionContext;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.ext.snowflake.SnowflakeConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPConnectionBootstrap;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.exec.DBCCachedContextDefaults;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;

class SnowflakeExecutionContext extends GenericExecutionContext {
    private static final Log log = Log.getLog(SnowflakeExecutionContext.class);

    @Nullable
    private String activeDatabaseName;
    @Nullable
    private String activeSchemaName;

    SnowflakeExecutionContext(JDBCRemoteInstance instance, String purpose) {
        super(instance, purpose);
    }

    @Nullable
    @Override
    public GenericCatalog getDefaultCatalog() {
        if (CommonUtils.isEmpty(activeDatabaseName)) {
            return null;
        }
        return getDataSource().getCatalog(activeDatabaseName);
    }

    @Nullable
    @Override
    public GenericSchema getDefaultSchema() {
        if (CommonUtils.isEmpty(activeSchemaName)) {
            return null;
        }
        GenericCatalog defaultCatalog = getDefaultCatalog();
        if (defaultCatalog == null) {
            return null;
        }
        try {
            return defaultCatalog.getSchema(new VoidProgressMonitor(), activeSchemaName);
        } catch (DBException e) {
            log.error("Unable to retrieve active schema by its name", e);
            return null;
        }
    }

    @Override
    public boolean supportsCatalogChange() {
        return true;
    }

    @Override
    public boolean supportsSchemaChange() {
        return true;
    }

    @Override
    public void setDefaultCatalog(DBRProgressMonitor monitor, @NotNull GenericCatalog catalog, @Nullable GenericSchema schema) throws DBCException {
        setDefaultCatalog(monitor, catalog, schema, false);
    }

    void setDefaultCatalog(DBRProgressMonitor monitor, @NotNull GenericCatalog catalog, @Nullable DBSObject schema, boolean force)
            throws DBCException {
        String catalogName = catalog.getName();
        if (!force && catalogName.equals(activeDatabaseName) && (schema == null || schema.getName().equals(activeSchemaName))) {
            return;
        }
        DBSObject oldActiveDatabase = getDefaultCatalog();
        setActiveDatabase(monitor, catalogName);

        try {
            catalog.getSchemas(monitor);
        } catch (DBException e) {
            log.debug("Error caching database schemas", e);
        }
        activeDatabaseName = catalogName;

        DBUtils.fireObjectSelectionChange(oldActiveDatabase, catalog, this);

        if (schema != null) {
            setDefaultSchema(monitor, schema, force);
        }
    }

    @Override
    public void setDefaultSchema(DBRProgressMonitor monitor, @NotNull GenericSchema newActiveSchema) throws DBCException {
        setDefaultSchema(monitor, newActiveSchema, false);
    }

    void setDefaultSchema(DBRProgressMonitor monitor, @NotNull DBSObject newActiveSchema, boolean force) throws DBCException {
        String newSchemaName = newActiveSchema.getName();
        if (!force && newSchemaName.equals(activeSchemaName)) {
            return;
        }
        setActiveSchema(monitor, newSchemaName);
        GenericSchema oldActiveSchema = getDefaultSchema();
        activeSchemaName = newSchemaName;
        DBUtils.fireObjectSelectionChange(oldActiveSchema, newActiveSchema, this);
    }

    @Override
    public boolean refreshDefaults(DBRProgressMonitor monitor, boolean useBootstrapSettings) throws DBException {
        boolean isRefreshed = false;
        String currentDatabase = null;
        String currentSchema = null;

        try (JDBCSession session = openSession(monitor, DBCExecutionPurpose.META, "Query active database and schema")) {
            try (JDBCStatement dbStat = session.createStatement()) {
                try (JDBCResultSet dbResult = dbStat.executeQuery("SELECT CURRENT_DATABASE(), CURRENT_SCHEMA()")) {
                    if (dbResult != null) {
                        dbResult.next();
                        currentDatabase = dbResult.getString(1);
                        currentSchema = dbResult.getString(2);
                    }
                }
            } catch (SQLException e) {
                log.debug("Exception caught when refreshing defaults for Snowflake execution context", e);
                throw new DBException("Unable to refresh defaults for Snowflake execution context", e);
            }

            if (useBootstrapSettings) {
                DBPConnectionBootstrap bootstrap = getBootstrapSettings();
                DBPConnectionConfiguration connectionConfiguration = getDataSource().getContainer().getConnectionConfiguration();
                if (!CommonUtils.isEmpty(bootstrap.getDefaultCatalogName()) && CommonUtils.isEmpty(connectionConfiguration.getProviderProperty(SnowflakeConstants.PROP_SCHEMA))) {
                    setActiveDatabase(monitor, bootstrap.getDefaultCatalogName());
                    currentDatabase = bootstrap.getDefaultCatalogName();
                }
                if (!CommonUtils.isEmpty(bootstrap.getDefaultSchemaName()) && CommonUtils.isEmpty(connectionConfiguration.getDatabaseName())) {
                    setActiveSchema(monitor, bootstrap.getDefaultSchemaName());
                    currentSchema = bootstrap.getDefaultSchemaName();
                }
            }
        }

        if (!CommonUtils.isEmpty(currentDatabase) && !CommonUtils.equalObjects(currentDatabase, activeDatabaseName)) {
            activeDatabaseName = currentDatabase;
            isRefreshed = true;
        }
        if (!CommonUtils.isEmpty(currentSchema) && !CommonUtils.equalObjects(currentSchema, activeSchemaName)) {
            activeSchemaName = currentSchema;
            isRefreshed = true;
        }
        if (CommonUtils.isEmpty(currentSchema)) {
            activeSchemaName = "PUBLIC";
            isRefreshed = true;
        }

        return isRefreshed;
    }

    private void setActiveDatabase(DBRProgressMonitor monitor, @NotNull String databaseName) throws DBCException {
        try (JDBCSession session = openSession(monitor, DBCExecutionPurpose.UTIL, "Set active database")) {
            try (JDBCStatement dbStat = session.createStatement()) {
                dbStat.executeUpdate("USE DATABASE " + DBUtils.getQuotedIdentifier(getDataSource(), databaseName));
            }
        } catch (SQLException e) {
            log.error("Unable to set active database due to unexpected SQLException. databaseName=" + databaseName);
            throw new DBCException(e, this);
        }
    }

    private void setActiveSchema(DBRProgressMonitor monitor, @NotNull String schemaName) throws DBCException {
        try (JDBCSession session = openSession(monitor, DBCExecutionPurpose.UTIL, TASK_TITLE_SET_SCHEMA)) {
            try (JDBCStatement dbStat = session.createStatement()) {
                dbStat.executeUpdate("USE SCHEMA " + DBUtils.getQuotedIdentifier(getDataSource(), schemaName));
            }
        } catch (SQLException e) {
            log.error("Unable to set active schema due to unexpected SQLException. schemaName=" + schemaName);
            throw new DBCException(e, this);
        }
    }
    @NotNull
    @Override
    public DBCCachedContextDefaults getCachedDefault() {
        return new DBCCachedContextDefaults(activeDatabaseName, activeSchemaName);
    }
}
