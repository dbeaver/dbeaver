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
package org.jkiss.dbeaver.ext.duckdb.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericCatalog;
import org.jkiss.dbeaver.ext.generic.model.GenericExecutionContext;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.model.connection.DBPConnectionBootstrap;
import org.jkiss.dbeaver.model.exec.DBCCachedContextDefaults;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DuckDBExecutionContext extends GenericExecutionContext {

    private static final Logger log = LoggerFactory.getLogger(DuckDBExecutionContext.class);
    private String activeCatalogName;

    public DuckDBExecutionContext(@NotNull JDBCRemoteInstance instance, String purpose) {
        super(instance, purpose);
    }

    @Override
    public boolean supportsCatalogChange() {
        return true;
    }

    @Override
    public GenericCatalog getDefaultCatalog() {
        return activeCatalogName == null ? null : getDataSource().getCatalog(activeCatalogName);
    }

    @Override
    public boolean refreshDefaults(DBRProgressMonitor monitor, boolean useBootstrapSettings) throws DBException {
        boolean changed = super.refreshDefaults(monitor, useBootstrapSettings);

        if (useBootstrapSettings) {
            DBPConnectionBootstrap bootstrap = getBootstrapSettings();
            String bootstrapCatalogName = bootstrap.getDefaultCatalogName();

            if (!CommonUtils.isEmpty(bootstrapCatalogName) && !bootstrapCatalogName.equals(activeCatalogName)) {
                activeCatalogName = bootstrapCatalogName;
                changed = true;
            }
        }

        if (supportsCatalogChange() && CommonUtils.isEmpty(activeCatalogName)) {
            try (JDBCSession session = openSession(monitor, DBCExecutionPurpose.META, "Query active catalog")) {
                String currentCatalog = JDBCUtils.queryString(session, "SELECT current_catalog()");
                if (!CommonUtils.isEmpty(currentCatalog) && !currentCatalog.equals(activeCatalogName)) {
                    activeCatalogName = currentCatalog;
                    changed = true;
                }
            } catch (Exception e) {
                log.warn("Error while setting active catalog", e);
            }
        }

        return changed;
    }

    @Override
    public void setDefaultCatalog(DBRProgressMonitor monitor, GenericCatalog catalog, GenericSchema schema) throws DBCException {
        if (catalog == null) {
            return;
        }

        String newCatalogName = catalog.getName();
        if (CommonUtils.equalObjects(activeCatalogName, newCatalogName)) {
            return;
        }

        try (JDBCSession session = openSession(monitor, DBCExecutionPurpose.UTIL, "Set active catalog")) {
            JDBCUtils.executeSQL(session, "USE " + newCatalogName);
            activeCatalogName = newCatalogName;
        } catch (Exception e) {
            try {
                throw new DBException("Failed to set active catalog to " + newCatalogName, e);
            } catch (DBException ex) {
                log.warn("Error setting active catalog", ex);
            }
        }

        super.setDefaultCatalog(monitor, catalog, schema);
    }

    @Override
    public void initDefaultsFrom(DBRProgressMonitor monitor, GenericExecutionContext context) {
        try {
            super.initDefaultsFrom(monitor, context);
        } catch (DBCException e) {
            log.warn("Error initializing defaults from context", e);
        }

        if (context instanceof DuckDBExecutionContext duckDBContext) {
            this.activeCatalogName = duckDBContext.activeCatalogName;
        }
    }

    @NotNull
    @Override
    public DBCCachedContextDefaults getCachedDefault() {
        return new DBCCachedContextDefaults(activeCatalogName, getDefaultSchemaCached());
    }
}