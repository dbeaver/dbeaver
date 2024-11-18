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
package org.jkiss.dbeaver.ext.kingbase.model;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.kingbase.KingbaseConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPConnectionBootstrap;
import org.jkiss.dbeaver.model.dpi.DPIContainer;
import org.jkiss.dbeaver.model.dpi.DPIElement;
import org.jkiss.dbeaver.model.exec.DBCCachedContextDefaults;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

/**
 * KingbaseExecutionContext
 */
public class KingbaseExecutionContext extends JDBCExecutionContext implements DBCExecutionContextDefaults<KingbaseDatabase, KingbaseSchema> {

    private final List<String> searchPath = new ArrayList<>();
    private List<String> defaultSearchPath = new ArrayList<>();
    private String activeUser;
    private long activeSchemaId;
    private boolean isolatedContext;

    public KingbaseExecutionContext(@NotNull KingbaseDatabase database, String purpose) {
        super(database, purpose);
    }

    @DPIContainer
    @NotNull
    @Override
    public KingbaseDataSource getDataSource() {
        return (KingbaseDataSource) super.getDataSource();
    }

    @DPIElement
    @Nullable
    @Override
    public KingbaseExecutionContext getContextDefaults() {
        return this;
    }

    @NotNull
    @Override
    public KingbaseDatabase getDefaultCatalog() {
        return (KingbaseDatabase) getOwnerInstance();
    }
    @Override
    public KingbaseSchema getDefaultSchema() {
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
    public void setDefaultCatalog(DBRProgressMonitor monitor, KingbaseDatabase catalog, KingbaseSchema schema) throws DBCException {
        setDefaultCatalog(monitor, catalog, schema, false);
    }

    void setDefaultCatalog(@NotNull DBRProgressMonitor monitor, @NotNull KingbaseDatabase catalog, @Nullable KingbaseSchema schema, boolean force)
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
                    getDataSource().setActiveDatabase(catalog, this);
                }
                catalogChanged = true;
            }
            if (schema != null) {
                if (catalogChanged && !isolatedContext) {
                    // Catalog has been changed. Get the new one and change schema there
                    KingbaseDatabase newInstance = getDataSource().getDefaultInstance();
                    KingbaseExecutionContext newContext = (KingbaseExecutionContext) newInstance.getDefaultContext(false);
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
    public void setDefaultSchema(DBRProgressMonitor monitor, KingbaseSchema schema) throws DBCException {
        setDefaultCatalog(monitor, schema.getDatabase(), schema, false);
    }

    boolean changeDefaultSchema(DBRProgressMonitor monitor, KingbaseSchema schema, boolean reflect, boolean force) throws DBCException {
        if (activeSchemaId == schema.getObjectId() && !force) {
            return false;
        }
        if (schema.isExternal()) {
            return false;
        }

        setSearchPath(monitor, schema);
        setSearchPath(schema.getName());

        final KingbaseSchema oldActiveSchema = getDefaultSchema();

        this.activeSchemaId = schema.getObjectId();

        if (reflect) {
            DBUtils.fireObjectSelectionChange(oldActiveSchema, schema, this);
        }

        return true;
    }

    @Override
    public boolean refreshDefaults(DBRProgressMonitor monitor, boolean useBootstrapSettings) throws DBException {
        this.activeSchemaId = 0;
        try (JDBCSession session = openSession(monitor, DBCExecutionPurpose.META, "Read context defaults")) {
            try (JDBCPreparedStatement stat = session.prepareStatement("SELECT current_schema(),session_user")) {
                try (JDBCResultSet rs = stat.executeQuery()) {
                    if (rs.nextRow()) {
                        String activeSchemaName = JDBCUtils.safeGetString(rs, 1);
                        if (!CommonUtils.isEmpty(activeSchemaName)) {
                            // Pre-cache schemas, we need them anyway
                            getDefaultCatalog().getSchemas(monitor);
                            final KingbaseSchema activeSchema = getDefaultCatalog().getSchema(monitor, activeSchemaName);
                            if (activeSchema != null) {
                                activeSchemaId = activeSchema.getObjectId();
                            }
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
                if (activeSchemaId == 0) {
                    for (String schemaName : searchPath) {
                        final KingbaseSchema activeSchema = getDefaultCatalog().getSchema(monitor, schemaName);
                        if (activeSchema != null) {
                            activeSchemaId = activeSchema.getObjectId();
                            break;
                        }
                    }
                }
            } else {
                this.searchPath.add(KingbaseConstants.PUBLIC_SCHEMA_NAME);
            }

            if (defaultSearchPath.isEmpty()) {
                setUserInTheEndOfThePath(searchPath);
                defaultSearchPath = new ArrayList<>(searchPath);
            }

            if (useBootstrapSettings) {
                DBPConnectionBootstrap bootstrap = getBootstrapSettings();
                String bsSchemaName = bootstrap.getDefaultSchemaName();
                if (!CommonUtils.isEmpty(bsSchemaName)) {
                    setSearchPath(monitor, bsSchemaName);
                    KingbaseSchema bsSchema = getDefaultCatalog().getSchema(monitor, bsSchemaName);
                    if (bsSchema != null) {
                        activeSchemaId = bsSchema.getObjectId();
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

    private void setSearchPath(DBRProgressMonitor monitor, KingbaseSchema schema) throws DBCException {
        setSearchPath(monitor, schema.getName());
    }

    private void setSearchPath(DBRProgressMonitor monitor, String defSchemaName) throws DBCException {
        List<String> newSearchPath = new ArrayList<>(getDefaultSearchPath());
        int schemaIndex = newSearchPath.indexOf(defSchemaName);
        if (schemaIndex > 0) {
            newSearchPath.remove(schemaIndex);
        }
        newSearchPath.add(0, defSchemaName);

        StringBuilder spString = new StringBuilder();
        for (String sp : newSearchPath) {
            if (spString.length() > 0) spString.append(",");
            spString.append(DBUtils.getQuotedIdentifier(getDataSource(), sp));
        }
        try (JDBCSession session = openSession(monitor, DBCExecutionPurpose.UTIL, "Change search path")) {
            DBExecUtils.tryExecuteRecover(session, session.getDataSource(), param -> {
                try {
                    JDBCUtils.executeSQL(session, "SET search_path = " + spString);
                } catch (SQLException e) {
                    throw new InvocationTargetException(e);
                }
            });
        } catch (DBException e) {
            throw new DBCException("Error setting search path", e, this);
        }
    }

    private static boolean isUserFirstInPath(List<String> newSearchPath) {
        return !newSearchPath.isEmpty() && newSearchPath.get(0).equals(KingbaseConstants.USER_VARIABLE);
    }

    private void setUserInTheEndOfThePath(List<String> searchPath) {
        if (CommonUtils.isEmpty(searchPath)) {
            return;
        }
        if (isUserFirstInPath(searchPath)) {
            searchPath.remove(0);
            searchPath.add(KingbaseConstants.USER_VARIABLE);
        } else {
            int userIndex = -1;
            for (int i = 0; i < searchPath.size(); i++) {
                if (searchPath.get(i).equals(KingbaseConstants.USER_VARIABLE)) {
                    userIndex = i;
                    break;
                }
            }
            if (userIndex != -1) {
                searchPath.remove(userIndex);
                searchPath.add(KingbaseConstants.USER_VARIABLE);
            }
        }
    }

    private void setSearchPath(String path) {
        searchPath.clear();
        searchPath.add(path);
        if (!path.equals(activeUser)) {
            searchPath.add(activeUser);
        }
    }

    private void setSessionRole(@NotNull DBRProgressMonitor monitor) throws DBCException {
        final String roleName = getDataSource().getContainer().getConnectionConfiguration().getProviderProperty(KingbaseConstants.PROP_CHOSEN_ROLE);
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
