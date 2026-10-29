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
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaObject;
import org.jkiss.dbeaver.ext.sqlite.model.SQLiteDataSource;
import org.jkiss.dbeaver.ext.sqlite.model.SQLiteDataSourceInfo;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCInvalidatePhase;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

public class CloudflareD1DataSource extends SQLiteDataSource {
    private final Map<String, CloudflareD1Database> databases = new LinkedHashMap<>();
    private final Map<String, CloudflareD1Instance> instances = new LinkedHashMap<>();

    public CloudflareD1DataSource(
        @NotNull DBRProgressMonitor monitor, @NotNull DBPDataSourceContainer container, @NotNull GenericMetaModel metaModel
    ) throws DBException {
        super(monitor, container, metaModel);
    }

    @NotNull
    @Override
    public List<String> getCatalogsNames(
        @NotNull DBRProgressMonitor monitor,
        @NotNull JDBCDatabaseMetaData metaData,
        @Nullable GenericMetaObject catalogObject,
        @Nullable DBSObjectFilter catalogFilters
    ) throws DBException {
        List<CloudflareD1Database> loaded = CloudflareD1ConnectionAdapter.list(monitor, this);
        this.databases.clear();
        this.catalogsFiltered = false;
        List<String> names = new ArrayList<>();
        for (CloudflareD1Database database : loaded) {
            this.databases.put(database.name(), database);
            if (catalogFilters == null || catalogFilters.matches(database.name())) {
                names.add(database.name());
            } else {
                this.catalogsFiltered = true;
            }
        }
        return names;
    }

    @NotNull
    public CloudflareD1Database getDatabase(@NotNull String name) {
        return Objects.requireNonNull(this.databases.get(name), "Unknown D1 database: " + name);
    }

    @NotNull
    synchronized CloudflareD1Instance getInstance(@NotNull CloudflareD1Database database) {
        return this.instances.computeIfAbsent(database.id(), id -> new CloudflareD1Instance(this, id));
    }

    public void closeCatalogContexts(@NotNull DBRProgressMonitor monitor, @NotNull String databaseId) throws DBException {
        CloudflareD1Instance instance;
        String name = this.databases.values().stream()
            .filter(database -> databaseId.equals(database.id()))
            .map(CloudflareD1Database::name)
            .findFirst()
            .orElse(null);
        if (name != null) {
            for (JDBCExecutionContext context : this.getDefaultInstance().getAllContexts()) {
                if (context instanceof CloudflareD1ExecutionContext d1 && name.equals(d1.getDefaultCatalogCached())) {
                    context.invalidateContext(monitor, DBCInvalidatePhase.BEFORE_INVALIDATE);
                }
            }
        }
        synchronized (this) {
            instance = this.instances.remove(databaseId);
        }
        if (instance != null) {
            instance.shutdown(monitor);
        }
    }

    public void invalidateCatalogCaches(@NotNull DBRProgressMonitor monitor, @NotNull CloudflareD1Catalog catalog) throws DBException {
        GenericCatalog cached = this.getCatalog(catalog.getName());
        if (cached != null) {
            var schemas = cached.getSchemas(monitor);
            if (schemas != null) {
                for (var schema : schemas) {
                    schema.refreshObject(monitor);
                }
            }
            cached.refreshObject(monitor);
        }
        if (cached != catalog) {
            catalog.refreshObject(monitor);
        }
        DBUtils.fireObjectUpdate(catalog);
    }

    @NotNull
    @Override
    public synchronized List<? extends JDBCRemoteInstance> getAvailableInstances() {
        List<JDBCRemoteInstance> result = new ArrayList<>(super.getAvailableInstances());
        if (this.instances != null) {
            result.addAll(this.instances.values());
        }
        return result;
    }

    @NotNull
    @Override
    protected JDBCExecutionContext createExecutionContext(@NotNull JDBCRemoteInstance instance, @NotNull String type) {
        return new CloudflareD1ExecutionContext(instance, type);
    }

    @NotNull
    @Override
    protected Connection openConnection(
        @NotNull DBRProgressMonitor monitor,
        @NotNull JDBCExecutionContext context,
        @NotNull DBPConnectionConfiguration connectionInfo,
        @NotNull String purpose
    ) throws DBCException {
        if (context instanceof CloudflareD1ExecutionContext d1) {
            connectionInfo = d1.prepareConfiguration(connectionInfo);
        }
        return super.openConnection(monitor, context, connectionInfo, purpose);
    }

    @NotNull
    @Override
    protected Map<String, String> getInternalConnectionProperties(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDriver driver,
        @Nullable JDBCExecutionContext context,
        @NotNull String purpose,
        @NotNull DBPConnectionConfiguration configuration
    ) {
        return Map.of();
    }

    @NotNull
    @Override
    protected Properties getAllConnectionProperties(
        @NotNull DBRProgressMonitor monitor,
        @Nullable JDBCExecutionContext context,
        @NotNull String purpose,
        @NotNull DBPConnectionConfiguration configuration
    ) throws DBCException {
        Properties properties = super.getAllConnectionProperties(monitor, context, purpose, configuration);
        properties.remove("accountId");
        properties.remove("apiToken");
        return properties;
    }

    @NotNull
    @Override
    protected DBPDataSourceInfo createDataSourceInfo(@NotNull DBRProgressMonitor monitor, @NotNull JDBCDatabaseMetaData metaData) {
        return new SQLiteDataSourceInfo(getContainer().getDriver(), metaData) {
            @Override
            public boolean supportsCatalogSelection() {
                return true;
            }

            @Override
            public boolean needsTableMetaForColumnResolution() {
                return true;
            }
        };
    }

    @NotNull
    @Override
    public Class<? extends DBSObject> getPrimaryChildType(@Nullable DBRProgressMonitor monitor) {
        return CloudflareD1Catalog.class;
    }

    @Override
    public boolean isOmitSchema() {
        return false;
    }
}
