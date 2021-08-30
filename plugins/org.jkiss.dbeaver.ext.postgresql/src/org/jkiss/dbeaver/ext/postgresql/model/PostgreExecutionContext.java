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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPConnectionBootstrap;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * PostgreExecutionContext
 */
public class PostgreExecutionContext extends JDBCExecutionContext implements DBCExecutionContextDefaults<PostgreDatabase, PostgreSchema> {
    private PostgreSchema activeSchema;
    private final List<String> searchPath = new ArrayList<>();
    private List<String> defaultSearchPath = new ArrayList<>();
    private String activeUser;
    private boolean isolatedContext;

    public PostgreExecutionContext(@NotNull PostgreDatabase database, String purpose) {
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

    @Override
    public PostgreDatabase getDefaultCatalog() {
        return (PostgreDatabase) getOwnerInstance();
    }

    @Override
    public PostgreSchema getDefaultSchema() {
        return activeSchema;
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
    public void setDefaultCatalog(DBRProgressMonitor monitor, PostgreDatabase catalog, PostgreSchema schema) throws DBCException {
        setDefaultCatalog(monitor, catalog, schema, false);
    }

    void setDefaultCatalog(@NotNull DBRProgressMonitor monitor, @NotNull PostgreDatabase catalog, @Nullable PostgreSchema schema, boolean force)
            throws DBCException {
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
                    getDataSource().setActiveDatabase(catalog);
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
                DBUtils.fireObjectSelectionChange(oldInstance, catalog);
            }
        } catch (DBException e) {
            throw new DBCException("Error changing default database", e);
        }
    }

    @Override
    public void setDefaultSchema(DBRProgressMonitor monitor, PostgreSchema schema) throws DBCException {
        setDefaultCatalog(monitor, schema.getDatabase(), schema, false);
    }

    boolean changeDefaultSchema(DBRProgressMonitor monitor, PostgreSchema schema, boolean reflect, boolean force) throws DBCException {
        PostgreSchema oldActiveSchema = this.activeSchema;
        if (oldActiveSchema == schema && !force) {
            return false;
        }
        if (schema.isExternal()) {
            return false;
        }

        setSearchPath(monitor, schema);
        this.activeSchema = schema;
        setSearchPath(schema.getName());

        if (reflect) {
            DBUtils.fireObjectSelectionChange(oldActiveSchema, activeSchema);
        }

        return true;
    }

    @Override
    public boolean refreshDefaults(DBRProgressMonitor monitor, boolean useBootstrapSettings) throws DBException {
        // Check default active schema
        try (JDBCSession session = openSession(monitor, DBCExecutionPurpose.META, "Read context defaults")) {
            try (JDBCPreparedStatement stat = session.prepareStatement("SELECT current_schema(),session_user")) {
                try (JDBCResultSet rs = stat.executeQuery()) {
                    if (rs.nextRow()) {
                        String activeSchemaName = JDBCUtils.safeGetString(rs, 1);
                        if (!CommonUtils.isEmpty(activeSchemaName)) {
                            // Pre-cache schemas, we need them anyway
                            getDefaultCatalog().getSchemas(monitor);
                            activeSchema = getDefaultCatalog().getSchema(monitor, activeSchemaName);
                        }
                        activeUser = JDBCUtils.safeGetString(rs, 2);
                    }
                }
            }
            String searchPathStr = JDBCUtils.queryString(session, "SHOW search_path");
            this.searchPath.clear();
            if (searchPathStr != null) {
                for (String str : searchPathStr.split(",")) {
                    str = str.trim();
                    String spSchema = DBUtils.getUnQuotedIdentifier(getDataSource(), str);
                    if (!searchPath.contains(spSchema)) {
                        this.searchPath.add(spSchema);
                    }
                }
                if (activeSchema == null) {
                    // This may happen
                    for (String schemaName : searchPath) {
                        activeSchema = getDefaultCatalog().getSchema(monitor, schemaName);
                        if (activeSchema != null) {
                            break;
                        }
                    }
                }
            } else {
                this.searchPath.add(PostgreConstants.PUBLIC_SCHEMA_NAME);
            }

            if (defaultSearchPath.isEmpty()) {
                defaultSearchPath = new ArrayList<>(searchPath);
            }

            if (useBootstrapSettings) {
                DBPConnectionBootstrap bootstrap = getBootstrapSettings();
                String bsSchemaName = bootstrap.getDefaultSchemaName();
                if (!CommonUtils.isEmpty(bsSchemaName)) {
                    setSearchPath(monitor, bsSchemaName);
                    PostgreSchema bsSchema = getDefaultCatalog().getSchema(monitor, bsSchemaName);
                    if (bsSchema != null) {
                        activeSchema = bsSchema;
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBCException(e, this);
        }
        setSessionRole(monitor);
        return true;
    }

    public String getActiveUser() {
        return activeUser;
    }

    public List<String> getSearchPath() {
        return searchPath;
    }

    List<String> getDefaultSearchPath() {
        return defaultSearchPath;
    }

    private void setSearchPath(DBRProgressMonitor monitor, PostgreSchema schema) throws DBCException {
        // Construct search path from current search path but put default schema first
        setSearchPath(monitor, schema.getName());
    }

    private void setSearchPath(DBRProgressMonitor monitor, String defSchemaName) throws DBCException {
        List<String> newSearchPath = new ArrayList<>(getDefaultSearchPath());
        int schemaIndex = newSearchPath.indexOf(defSchemaName);
        /*if (schemaIndex == 0 || (schemaIndex == 1 && isUserFirstInPath(newSearchPath))) {
            // Already default schema
            return;
        } else*/
        {
            if (schemaIndex > 0) {
                // Remove from previous position
                newSearchPath.remove(schemaIndex);
            }
            // Add it first (or after $user)
            int newIndex = isUserFirstInPath(newSearchPath) ? 1 : 0;
            newSearchPath.add(newIndex, defSchemaName);
        }

        StringBuilder spString = new StringBuilder();
        for (String sp : newSearchPath) {
            if (spString.length() > 0) spString.append(",");
            spString.append(DBUtils.getQuotedIdentifier(getDataSource(), sp));
        }
        try (JDBCSession session = openSession(monitor, DBCExecutionPurpose.UTIL, "Change search path")) {
            JDBCUtils.executeSQL(session, "SET search_path = " + spString);
        } catch (SQLException e) {
            throw new DBCException("Error setting search path", e, this);
        }
    }

    private static boolean isUserFirstInPath(List<String> newSearchPath) {
        return !newSearchPath.isEmpty() && newSearchPath.get(0).equals("$user");
    }

    private void setSearchPath(String path) {
        searchPath.clear();
        searchPath.add(path);
        if (!path.equals(activeUser)) {
            searchPath.add(activeUser);
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
}
