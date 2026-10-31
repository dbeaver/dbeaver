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
package org.jkiss.dbeaver.ext.cloudflare.d1.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericCatalog;
import org.jkiss.dbeaver.ext.generic.model.GenericExecutionContext;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.exec.DBCCachedContextDefaults;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

final class CloudflareD1ExecutionContext extends GenericExecutionContext {
    private static final String URL_PREFIX = "jdbc:d1://";
    private final String databaseId;
    private String catalogName;

    CloudflareD1ExecutionContext(@NotNull JDBCRemoteInstance instance, @NotNull String purpose) {
        super(instance, purpose);
        databaseId = instance instanceof CloudflareD1Instance d1 ? d1.getDatabaseId() : null;
    }

    @Nullable
    @Override
    public synchronized Connection getConnection(@NotNull DBRProgressMonitor monitor, boolean openIfNeeded) throws SQLException {
        return super.getConnection(monitor, openIfNeeded);
    }

    @NotNull
    DBPConnectionConfiguration prepareConfiguration(@NotNull DBPConnectionConfiguration original) {
        DBPConnectionConfiguration configuration = new DBPConnectionConfiguration(original);
        if (this.databaseId != null) {
            configuration.setDatabaseName(this.databaseId);
            configuration.setUrl(URL_PREFIX + this.databaseId);
        }
        return configuration;
    }

    @Nullable
    @Override
    public GenericCatalog getDefaultObject() {
        return this.getDefaultCatalog();
    }

    @Nullable
    @Override
    public String getDefaultCatalogCached() {
        return this.catalogName;
    }

    @Nullable
    @Override
    public String getDefaultSchemaCached() {
        return null;
    }

    @Nullable
    @Override
    public GenericCatalog getDefaultCatalog() {
        return this.catalogName == null ? null : this.getDataSource().getCatalog(this.catalogName);
    }

    @Nullable
    @Override
    public GenericSchema getDefaultSchema() {
        return null;
    }

    @Override
    public boolean supportsCatalogChange() {
        return this.databaseId == null;
    }

    @Override
    public boolean supportsSchemaChange() {
        return false;
    }

    @Override
    public void setDefaultCatalog(
        @NotNull DBRProgressMonitor monitor, @NotNull GenericCatalog catalog, @Nullable GenericSchema schema
    ) throws DBCException {
        if (databaseId != null && (!(catalog instanceof CloudflareD1Catalog d1) || !databaseId.equals(d1.getDatabaseId()))) {
            throw new DBCException("This context belongs to another D1 database");
        }
        GenericCatalog previous = this.getDefaultCatalog();
        try (JDBCSession session = openSession(monitor, DBCExecutionPurpose.UTIL, "Select D1 database")) {
            session.setCatalog(catalog.getName());
            this.catalogName = catalog.getName();
        } catch (SQLException e) {
            throw new DBCException(e, this);
        }
        DBUtils.fireObjectSelectionChange(previous, catalog, this);
    }

    @Override
    public boolean refreshDefaults(@NotNull DBRProgressMonitor monitor, boolean useBootstrapSettings) throws DBException {
        String previous = this.catalogName;
        try (JDBCSession session = openSession(monitor, DBCExecutionPurpose.META, "Read active D1 database")) {
            String configuredCatalog = this.getBootstrapSettings().getDefaultCatalogName();
            if (this.databaseId == null && useBootstrapSettings && configuredCatalog != null && !configuredCatalog.isBlank()) {
                session.setCatalog(configuredCatalog);
            }
            this.catalogName = session.getCatalog();
        } catch (SQLException e) {
            throw new DBCException(e, this);
        }
        return !Objects.equals(previous, this.catalogName);
    }

    @Override
    public void initDefaultsFrom(@NotNull DBRProgressMonitor monitor, @NotNull GenericExecutionContext context) throws DBCException {
        GenericCatalog catalog = context.getDefaultCatalog();
        if (this.databaseId == null && catalog != null) {
            this.setDefaultCatalog(monitor, catalog, null);
        } else {
            try {
                this.refreshDefaults(monitor, false);
            } catch (DBException e) {
                throw new DBCException(e, this);
            }
        }
    }

    @NotNull
    @Override
    public DBCCachedContextDefaults getCachedDefault() {
        return new DBCCachedContextDefaults(this.catalogName, null);
    }
}
