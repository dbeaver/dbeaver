/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPConnectionBootstrap;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * PostgreExecutionContext
 */
public class PostgreExecutionContext extends JDBCExecutionContext implements DBCExecutionContextDefaults<PostgreDatabase, PostgreSchema> {
    private final List<String> searchPath = new ArrayList<>();
    private String activeUser;
    private long activeSchemaId;
    private boolean isolatedContext;

    public PostgreExecutionContext(@NotNull PostgreDatabase database, @NotNull String purpose) {
        super(database, purpose);
    }

    @NotNull
    @Override
    public PostgreDataSource getDataSource() {
        return (PostgreDataSource) super.getDataSource();
    }

    @Nullable
    @Override
    public PostgreExecutionContext getContextDefaults() {
        return this;
    }

    @NotNull
    @Override
    public PostgreDatabase getDefaultCatalog() {
        // Get value from cached, used in getCachedDefault()
        return (PostgreDatabase) getOwnerInstance();
    }

    @Nullable
    @Override
    public PostgreSchema getDefaultSchema() {
        // Get value from cached, used in getCachedDefault()
        return getDefaultCatalog().getSchema(activeSchemaId);
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
    public void setDefaultCatalog(
        @NotNull DBRProgressMonitor monitor,
        @NotNull PostgreDatabase catalog,
        @Nullable PostgreSchema schema
    ) throws DBCException {
        setDefaultCatalog(monitor, catalog, schema, false);
    }

    void setDefaultCatalog(
        @NotNull DBRProgressMonitor monitor,
        @NotNull PostgreDatabase catalog,
        @Nullable PostgreSchema schema,
        boolean force
    ) throws DBCException {
        try {
            catalog.checkInstanceConnection(monitor);

            DBSObject oldInstance = getOwnerInstance();
            boolean catalogChanged = false, schemaChanged = false;
            if (oldInstance != catalog) {
                // Changing catalog means reconnect
                // Change it only for isolated editor contexts
                if (isolatedContext) {
                    disconnect();
                    setOwnerInstance(catalog);
                    connect(monitor, null, null, null, false);
                } else {
                    getDataSource().setActiveDatabase(catalog, this);
                }
                catalogChanged = true;
            }
            if (schema != null) {
                if (catalogChanged && !isolatedContext) {
                    // Catalog has been changed. Get the new one and change schema there
                    PostgreDatabase newInstance = getDataSource().getDefaultInstance();
                    PostgreExecutionContext newContext = (PostgreExecutionContext) newInstance.getDefaultContext(false);
                    newContext.changeDefaultSchema(monitor, schema, true, force);
                } else {
                    schemaChanged = changeDefaultSchema(monitor, schema, true, force);
                }
            }
            if (catalogChanged || schemaChanged) {
                DBUtils.fireObjectSelectionChange(oldInstance, catalog, this);
            }
        } catch (DBException e) {
            throw new DBCException("Error changing default database", e);
        }
    }

    @Override
    public void setDefaultSchema(@NotNull DBRProgressMonitor monitor, @NotNull PostgreSchema schema) throws DBCException {
        setDefaultCatalog(monitor, schema.getDatabase(), schema, false);
    }

    private boolean changeDefaultSchema(
        @NotNull DBRProgressMonitor monitor,
        @NotNull PostgreSchema schema,
        boolean reflect,
        boolean force
    ) throws DBException {
        if (activeSchemaId == schema.getObjectId() && !force) {
            return false;
        }
        if (schema.isExternal() || schema.isSystem()) {
            return false;
        }

        var oldActiveSchema = getDefaultSchema();
        setSearchPath(monitor, schema.getName());

        if (reflect) {
            DBUtils.fireObjectSelectionChange(oldActiveSchema, schema, this);
        }

        return true;
    }

    @Override
    public boolean refreshDefaults(@NotNull DBRProgressMonitor monitor, boolean useBootstrapSettings) throws DBException {
        this.activeSchemaId = 0;
        this.activeUser = null;
        this.searchPath.clear();

        // Check default active schema
        try (var session = openSession(monitor, DBCExecutionPurpose.META, "Read context defaults")) {
            monitor.subTask("Retrieve active search path");

            var searchPathStr = CommonUtils.notEmpty(JDBCUtils.queryString(session, "SHOW search_path"));
            for (String str : searchPathStr.split(",")) {
                searchPath.add(DBUtils.getUnQuotedIdentifier(getDataSource(), str.trim()));
            }

            monitor.subTask("Retrieve active schema and user");

            var result = JDBCUtils.queryStrings(session, "SELECT current_schema(),session_user");
            setActiveSchema(monitor, result.getFirst());
            setActiveUser(result.getLast());
        } catch (SQLException e) {
            throw new DBCException(e, this);
        }

        if (useBootstrapSettings) {
            DBPConnectionBootstrap bootstrap = getBootstrapSettings();
            String bsSchemaName = bootstrap.getDefaultSchemaName();
            if (!CommonUtils.isEmpty(bsSchemaName)) {
                setSearchPath(monitor, bsSchemaName);
            }
        }

        setSessionRole(monitor);
        return true;
    }

    private void setActiveSchema(@NotNull DBRProgressMonitor monitor, @Nullable String activeSchemaName) throws DBException {
        // Pre-cache schemas, we need them anyway
        getDefaultCatalog().getSchemas(monitor);

        if (CommonUtils.isNotEmpty(activeSchemaName)) {
            var activeSchema = getDefaultCatalog().getSchema(monitor, activeSchemaName);
            if (activeSchema != null) {
                activeSchemaId = activeSchema.getObjectId();
            }
        }

        if (activeSchemaId == 0) {
            // This may happen
            for (String schemaName : searchPath) {
                var activeSchema = getDefaultCatalog().getSchema(monitor, schemaName);
                if (activeSchema != null) {
                    activeSchemaId = activeSchema.getObjectId();
                    break;
                }
            }
        }
    }

    private void setActiveUser(@Nullable String activeUser) {
        this.activeUser = activeUser;
    }

    @Nullable
    public String getActiveUser() {
        return activeUser;
    }

    /**
     * Computes the effective {@code search_path}.
     * <p>
     * The path consists of two parts (from first used to last used):
     * <ul>
     *     <li>the selected schema, if present</li>
     *     <li>the default search_path retrieved from {@code pg_settings}</li>
     * </ul>
     *
     * @return the effective {@code search_path}
     */
    @NotNull
    public List<String> computeSearchPath() {
        var path = new ArrayList<String>(searchPath);

        var activeSchema = getDefaultSchema();
        if (activeSchema != null) {
            path.remove(activeSchema.getName());
            path.addFirst(activeSchema.getName());
        }

        return List.copyOf(path);
    }

    private void setSearchPath(@NotNull DBRProgressMonitor monitor, @NotNull String activeSchemaName) throws DBException {
        var activeSchema = getDefaultCatalog().getSchema(monitor, activeSchemaName);
        if (activeSchema != null) {
            activeSchemaId = activeSchema.getObjectId();
        }

        var searchPath = computeSearchPath().stream()
            .map(name -> DBUtils.getQuotedIdentifier(getDataSource(), name))
            .collect(Collectors.joining(","));

        try (JDBCSession session = openSession(monitor, DBCExecutionPurpose.UTIL, "Change search path")) {
            DBExecUtils.tryExecuteRecover(session, session.getDataSource(), param -> {
                try {
                    JDBCUtils.executeSQL(session, "SET search_path = " + searchPath);
                } catch (SQLException e) {
                    throw new InvocationTargetException(e);
                }
            });
        } catch (DBException e) {
            throw new DBCException("Error setting search path", e, this);
        }
    }

    private void setSessionRole(@NotNull DBRProgressMonitor monitor) throws DBCException {
        final String roleName = getDataSource().getContainer().getConnectionConfiguration().getProviderProperty(PostgreConstants.PROP_CHOSEN_ROLE);
        if (CommonUtils.isEmpty(roleName)) {
            return;
        }
        try (JDBCSession session = openSession(monitor, DBCExecutionPurpose.UTIL, "Set active role")) {
            try (JDBCStatement dbStat = session.createStatement()) {
                String sql = "SET ROLE " + getDataSource().getSQLDialect().getQuotedIdentifier(roleName, false, true);
                dbStat.executeUpdate(sql);
            }
        } catch (SQLException e) {
            throw new DBCException(e, this);
        }
    }

    public void setIsolatedContext(boolean isolatedContext) {
        this.isolatedContext = isolatedContext;
    }

    @NotNull
    @Override
    public DBCCachedContextDefaults getCachedDefault() {
        //Method get cashed value
        String schemaName = (getDefaultSchema() != null) ? getDefaultSchema().getName() : null;
        return new DBCCachedContextDefaults(getOwnerInstance().getName(), schemaName);
    }
}
