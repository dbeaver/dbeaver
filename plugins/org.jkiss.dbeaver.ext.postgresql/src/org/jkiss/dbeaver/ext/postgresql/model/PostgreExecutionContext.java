/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPConnectionBootstrap;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * PostgreExecutionContext
 */
public class PostgreExecutionContext extends JDBCExecutionContext implements DBCExecutionContextDefaults<PostgreDatabase, PostgreSchema> {
    private static final Log log = Log.getLog(PostgreExecutionContext.class);

    private PostgreSchema activeSchema;
    private final List<String> searchPath = new ArrayList<>();
    private List<String> defaultSearchPath = new ArrayList<>();
    private String activeUser;

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
    public DBCExecutionContextDefaults getContextDefaults() {
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

    void setDefaultsFrom(@NotNull PostgreExecutionContext initFrom) {
        this.activeUser = initFrom.activeUser;
        this.searchPath.clear();
        this.defaultSearchPath = new ArrayList<>(initFrom.defaultSearchPath);
    }

    @Override
    public void setDefaultCatalog(DBRProgressMonitor monitor, PostgreDatabase catalog, PostgreSchema schema) throws DBCException {
        PostgreDataSource dataSource = getDefaultCatalog().getDataSource();
        PostgreDatabase defaultInstance = dataSource.getDefaultInstance();
        try {
            JDBCRemoteInstance oldInstance = getOwnerInstance();
            if (oldInstance != catalog) {
                disconnect();
                setOwnerInstance(catalog);
                connect(monitor, null, null, null, false);
            }
            if (schema != null && !CommonUtils.equalObjects(schema, activeSchema)) {
                setDefaultSchema(monitor, schema);
            }
            DBUtils.fireObjectSelectionChange(oldInstance, catalog);
        } catch (DBException e) {
            throw new DBCException("Error changing default database", e);
        }
    }

    @Override
    public void setDefaultSchema(DBRProgressMonitor monitor, PostgreSchema schema) throws DBCException {
        setDefaultSchema(monitor, schema, true);
    }

    public void setDefaultSchema(DBRProgressMonitor monitor, PostgreSchema schema, boolean reflect) throws DBCException {
        PostgreSchema oldActiveSchema = this.activeSchema;
        if (oldActiveSchema == schema) {
            return;
        }

        setSearchPath(monitor, schema);
        this.activeSchema = schema;
        setSearchPath(schema.getName());

        if (reflect) {
            DBUtils.fireObjectSelectionChange(oldActiveSchema, activeSchema);
        }
    }

    @Override
    public boolean refreshDefaults(DBRProgressMonitor monitor, boolean useBootstrapSettings) throws DBException {
        // Check default active schema
        try (JDBCSession session = openSession(monitor, DBCExecutionPurpose.META, "Read context defaults")) {
            if (useBootstrapSettings) {
                DBPConnectionBootstrap bootstrap = getBootstrapSettings();
                if (!CommonUtils.isEmpty(bootstrap.getDefaultSchemaName())) {
                    setSearchPath(monitor, bootstrap.getDefaultSchemaName());
                }
            }

            try (JDBCPreparedStatement stat = session.prepareStatement("SELECT current_schema(),session_user")) {
                try (JDBCResultSet rs = stat.executeQuery()) {
                    if (rs.nextRow()) {
                        String activeSchemaName = JDBCUtils.safeGetString(rs, 1);
                        if (!CommonUtils.isEmpty(activeSchemaName)) {
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
                    this.searchPath.add(DBUtils.getUnQuotedIdentifier(getDataSource(), str));
                }
            } else {
                this.searchPath.add(PostgreConstants.PUBLIC_SCHEMA_NAME);
            }

            defaultSearchPath = new ArrayList<>(searchPath);
        } catch (SQLException e) {
            throw new DBCException(e, this);
        }

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
        if (schemaIndex == 0) {
            // Already default schema
        } else {
            if (schemaIndex > 0) {
                // Remove from previous position
                newSearchPath.remove(schemaIndex);
            }
            // Add it first
            newSearchPath.add(0, defSchemaName);
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

    private void setSearchPath(String path) {
        searchPath.clear();
        searchPath.add(path);
        if (!path.equals(activeUser)) {
            searchPath.add(activeUser);
        }
    }

}
