/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mssql.SQLServerConstants;
import org.jkiss.dbeaver.ext.mssql.SQLServerUtils;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPConnectionBootstrap;
import org.jkiss.dbeaver.model.dpi.DPIContainer;
import org.jkiss.dbeaver.model.exec.DBCCachedContextDefaults;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;

/**
 * SQLServerExecutionContext
 */
public class SQLServerExecutionContext extends JDBCExecutionContext implements DBCExecutionContextDefaults<SQLServerDatabase, SQLServerSchema> {
    private static final Log log = Log.getLog(SQLServerExecutionContext.class);

    //private SQLServerDatabase activeDatabase;
    private String activeDatabaseName;
    private String activeSchemaName;
    private String currentUser;

    SQLServerExecutionContext(@NotNull JDBCRemoteInstance instance, String purpose) {
        super(instance, purpose);
    }

    @DPIContainer
    @NotNull
    @Override
    public SQLServerDataSource getDataSource() {
        return (SQLServerDataSource) super.getDataSource();
    }

    @NotNull
    @Override
    public SQLServerExecutionContext getContextDefaults() {
        return this;
    }

    public String getActiveDatabaseName() {
        return activeDatabaseName;
    }

    @Override
    public SQLServerDatabase getDefaultCatalog() {
        return getDataSource().getDatabase(activeDatabaseName);
    }

    @Nullable
    @Override
    public SQLServerSchema getDefaultSchema() {
        if (CommonUtils.isEmpty(activeSchemaName)) {
            return null;
        }
        try {
            SQLServerDatabase defaultCatalog = getDefaultCatalog();
            if (defaultCatalog == null) {
                return null;
            }
            SQLServerSchema schema = defaultCatalog.getSchema(new VoidProgressMonitor(), activeSchemaName);
            if (schema == null) {
                // If DBO is not the default schema and default schema doesn't exist (or was filtered out) let's try DBO though
                schema = defaultCatalog.getSchema(new VoidProgressMonitor(), SQLServerConstants.DEFAULT_SCHEMA_NAME);
            }
            return schema;
        } catch (DBException e) {
            log.error(e);
            return null;
        }
    }

    @Override
    public boolean supportsCatalogChange() {
        return true;
    }

    @Override
    public boolean supportsSchemaChange() {
        return false;
    }

    @Override
    public void setDefaultCatalog(DBRProgressMonitor monitor, SQLServerDatabase catalog, @Nullable SQLServerSchema schema) throws DBCException {
        if (activeDatabaseName != null && activeDatabaseName.equals(catalog.getName())) {
            return;
        }
        final SQLServerDatabase oldActiveDatabase = getDefaultCatalog();

        if (!setCurrentDatabase(monitor, catalog)) {
            return;
        }
        try {
            catalog.getSchemas(monitor);
        } catch (DBException e) {
            log.debug("Error caching database schemas", e);
        }
        activeDatabaseName = catalog.getName();

        // Send notifications
        DBUtils.fireObjectSelectionChange(oldActiveDatabase, catalog, this);

        if (schema != null) {
            setDefaultSchema(monitor, schema);
        }
    }

    @Override
    public void setDefaultSchema(DBRProgressMonitor monitor, SQLServerSchema schema) throws DBCException {
        if (activeSchemaName != null && activeSchemaName.equals(schema.getName())) {
            return;
        }
        final SQLServerSchema oldActiveSchema = getDefaultSchema();

//        if (!setCurrentSchema(monitor, schema.getName())) {
//            return;
//        }
        activeSchemaName = schema.getName();

        // Send notifications
        DBUtils.fireObjectSelectionChange(oldActiveSchema, schema, this);
    }

    @Override
    public boolean refreshDefaults(DBRProgressMonitor monitor, boolean useBootstrapSettings) throws DBException {
        boolean refreshed = false;
        // Check default active schema
        try (JDBCSession session = openSession(monitor, DBCExecutionPurpose.META, "Query active schema and database")) {
            String currentDatabase = null;
            String currentSchema = null;
            try {
                try (JDBCStatement dbStat = session.createStatement()) {
                    String query = "SELECT db_name(), schema_name(), original_login()";
                    if (SQLServerUtils.isDriverBabelfish(session.getDataSource().getContainer().getDriver())) {
                        query = "SELECT db_name(), s.name AS schema_name, session_user AS original_login FROM sys.schemas s";
                    }
                    try (JDBCResultSet dbResult = dbStat.executeQuery(query)) {
                        dbResult.next();
                        currentDatabase = dbResult.getString(1);
                        currentSchema = dbResult.getString(2);
                        currentUser = dbResult.getString(3);
                    }
                }
            } catch (Throwable e) {
                log.debug("Error getting current user: " + e.getMessage());
            }
            if (!CommonUtils.isEmpty(currentDatabase) && (activeDatabaseName == null || !CommonUtils.equalObjects(currentDatabase, activeDatabaseName))) {
                activeDatabaseName = currentDatabase;
                refreshed = true;
            }
            if (CommonUtils.isEmpty(currentSchema)) {
                currentSchema = SQLServerConstants.DEFAULT_SCHEMA_NAME;
            }
            if (activeSchemaName == null || !CommonUtils.equalObjects(currentSchema, activeSchemaName)) {
                activeSchemaName = currentSchema;
                refreshed = true;
            }
            if (useBootstrapSettings) {
                DBPConnectionBootstrap bootstrap = getBootstrapSettings();
                if (!CommonUtils.isEmpty(bootstrap.getDefaultCatalogName()) && supportsCatalogChange() && !CommonUtils.equalObjects(bootstrap.getDefaultCatalogName(), activeDatabaseName)) {
                    setCurrentDatabase(monitor, bootstrap.getDefaultCatalogName());
                    refreshed = true;
                }
/*
                if (!CommonUtils.isEmpty(bootstrap.getDefaultSchemaName()) && supportsSchemaChange()) {
                    setCurrentSchema(monitor, bootstrap.getDefaultSchemaName());
                    refreshed = true;
                }
*/
            }
        }

        return refreshed;
    }

    boolean setCurrentDatabase(DBRProgressMonitor monitor, SQLServerDatabase object) throws DBCException {
        if (object == null) {
            log.debug("Null current schema");
            return false;
        }
        String databaseName = object.getName();
        return setCurrentDatabase(monitor, databaseName);
    }

    private boolean setCurrentDatabase(DBRProgressMonitor monitor, String databaseName) {
        try (JDBCSession session = openSession(monitor, DBCExecutionPurpose.UTIL, "Set active database")) {
            SQLServerUtils.setCurrentDatabase(session, databaseName);
            activeDatabaseName = databaseName;
            return true;
        } catch (SQLException e) {
            log.error(e);
            return false;
        }
    }

/*
    private boolean setCurrentSchema(DBRProgressMonitor monitor, String schemaName) {
        try (JDBCSession session = openSession(monitor, DBCExecutionPurpose.UTIL, "Set active schema")) {
            SQLServerUtils.setCurrentSchema(session, currentUser, schemaName);
            activeSchemaName = schemaName;
            return true;
        } catch (SQLException e) {
            log.error(e);
            return false;
        }
    }
*/

    @NotNull
    @Override
    public DBCCachedContextDefaults getCachedDefault() {
        return new DBCCachedContextDefaults(activeDatabaseName, activeSchemaName);
    }
}
