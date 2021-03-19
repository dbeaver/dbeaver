/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
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
        if (activeDatabaseName == null) {
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
    public void setDefaultCatalog(DBRProgressMonitor monitor, @NotNull GenericCatalog catalog, @Nullable GenericSchema schema)
            throws DBCException {
        if (activeDatabaseName != null && activeDatabaseName.equals(catalog.getName())) {
            return;
        }
        GenericCatalog oldActiveDatabase = getDefaultCatalog();
        setActiveDatabase(monitor, catalog.getName());

        try {
            catalog.getSchemas(monitor);
        } catch (DBException e) {
            log.debug("Error caching database schemas", e);
        }
        activeDatabaseName = catalog.getName();

        DBUtils.fireObjectSelectionChange(oldActiveDatabase, catalog);

        if (schema != null) {
            setDefaultSchema(monitor, schema);
        }
    }

    @Override
    public void setDefaultSchema(DBRProgressMonitor monitor, @NotNull GenericSchema newActiveSchema) throws DBCException {
        String newSchemaName = newActiveSchema.getName();
        if (activeSchemaName != null && activeSchemaName.equals(newSchemaName)) {
            return;
        }
        setActiveSchema(monitor, newSchemaName);
        GenericSchema oldActiveSchema = getDefaultSchema();
        activeSchemaName = newSchemaName;
        DBUtils.fireObjectSelectionChange(oldActiveSchema, newActiveSchema);
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

    private void setActiveDatabase(DBRProgressMonitor monitor, String databaseName) throws DBCException {
        try (JDBCSession session = openSession(monitor, DBCExecutionPurpose.UTIL, "Set active database")) {
            try (JDBCStatement dbStat = session.createStatement()) {
                dbStat.executeUpdate("USE DATABASE " + databaseName);
            }
        } catch (SQLException e) {
            throw new DBCException(e, this);
        }
    }

    private void setActiveSchema(DBRProgressMonitor monitor, String schemaName) throws DBCException {
        try (JDBCSession session = openSession(monitor, DBCExecutionPurpose.UTIL, "Set active schema")) {
            try (JDBCStatement dbStat = session.createStatement()) {
                dbStat.executeUpdate("USE SCHEMA " + schemaName);
            }
        } catch (SQLException e) {
            throw new DBCException(e, this);
        }
    }
}
