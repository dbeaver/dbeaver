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
import org.jkiss.dbeaver.ext.cloudflare.d1.internal.CloudflareD1ModelMessages;
import org.jkiss.dbeaver.ext.generic.model.GenericCatalog;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.model.DBPExclusiveResource;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSInstance;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class CloudflareD1Catalog extends GenericCatalog implements DBSInstance, DBPSaveableObject {
    private CloudflareD1Database database;
    private String name;
    private String jurisdiction;
    private String primaryLocationHint;
    private boolean persisted;
    private boolean appendToScope;
    private boolean detailsLoaded;
    private CloudflareD1ConnectionAdapter.ReplicationMode replicationMode;

    public CloudflareD1Catalog(@NotNull CloudflareD1DataSource dataSource, @NotNull String name) {
        super(dataSource, name);
        this.name = name;
    }

    public CloudflareD1Catalog(@NotNull CloudflareD1DataSource dataSource, @NotNull CloudflareD1Database database) {
        this(dataSource, database.name());
        this.setDatabase(database);
    }

    public void setDatabase(@NotNull CloudflareD1Database database) {
        this.database = database;
        this.name = database.name();
        this.jurisdiction = database.jurisdiction();
        this.replicationMode = database.readReplicationMode();
        this.detailsLoaded = false;
        this.persisted = true;
    }

    public synchronized void setDatabaseDetails(@NotNull CloudflareD1Database database) {
        this.setDatabase(database);
        this.detailsLoaded = true;
    }

    private synchronized void loadDetails(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (this.isPersisted() && !this.detailsLoaded) {
            this.setDatabaseDetails(this.readDetails(monitor));
        }
    }

    @NotNull
    protected CloudflareD1Database readDetails(@NotNull DBRProgressMonitor monitor) throws DBException {
        return CloudflareD1ConnectionAdapter.details(monitor, this);
    }

    @NotNull
    @Override
    public CloudflareD1DataSource getDataSource() {
        return (CloudflareD1DataSource) super.getDataSource();
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return this.name;
    }

    public void setName(@NotNull String name) {
        this.name = name;
    }

    @Nullable
    @Property(viewable = true, order = 2)
    public String getDatabaseId() {
        return this.database == null ? null : this.database.id();
    }

    @NotNull
    public String requireDatabaseId() {
        return Objects.requireNonNull(this.getDatabaseId(), "D1 database has not been created");
    }

    @Nullable
    public String getCreatedAt() {
        return this.database == null ? null : this.database.createdAt();
    }

    @Nullable
    public String getJurisdiction() {
        return this.jurisdiction;
    }

    public void setJurisdiction(@Nullable String jurisdiction) {
        this.jurisdiction = jurisdiction;
    }

    @Nullable
    public String getPrimaryLocationHint() {
        return this.primaryLocationHint;
    }

    public void setPrimaryLocationHint(@Nullable String primaryLocationHint) {
        this.primaryLocationHint = primaryLocationHint;
    }

    @Nullable
    public String getVersion() {
        return this.database == null ? null : this.database.version();
    }

    @Nullable
    public Long getFileSize() {
        return this.database == null ? null : this.database.fileSize();
    }

    @Nullable
    public String getRunningInRegion() {
        return this.database == null ? null : this.database.runningInRegion();
    }

    @Nullable
    public String getReadReplicationMode() {
        return this.replicationMode == null ? null : this.replicationMode.name();
    }

    @Nullable
    @Property(viewable = true, order = 3)
    public String getCreatedAt(@NotNull DBRProgressMonitor monitor) throws DBException {
        this.loadDetails(monitor);
        return this.getCreatedAt();
    }

    @Nullable
    @Property(viewable = true, order = 4)
    public String getJurisdiction(@NotNull DBRProgressMonitor monitor) throws DBException {
        this.loadDetails(monitor);
        return this.getJurisdiction();
    }

    @Nullable
    @Property(viewable = true, order = 5)
    public String getVersion(@NotNull DBRProgressMonitor monitor) throws DBException {
        this.loadDetails(monitor);
        return this.getVersion();
    }

    @Nullable
    @Property(viewable = true, order = 6)
    public Long getFileSize(@NotNull DBRProgressMonitor monitor) throws DBException {
        this.loadDetails(monitor);
        return this.getFileSize();
    }

    @NotNull
    @Property(viewable = true, order = 7)
    public String getRunningInRegion(@NotNull DBRProgressMonitor monitor) throws DBException {
        this.loadDetails(monitor);
        String region = this.getRunningInRegion();
        return region == null || region.isBlank() ? CloudflareD1ModelMessages.value_none : region;
    }

    @Nullable
    @Property(viewable = true, editable = true, updatable = true, order = 8)
    public CloudflareD1ConnectionAdapter.ReplicationMode getReadReplicationMode(@NotNull DBRProgressMonitor monitor) throws DBException {
        this.loadDetails(monitor);
        return this.replicationMode;
    }

    public void setReadReplicationMode(@Nullable CloudflareD1ConnectionAdapter.ReplicationMode mode) {
        this.replicationMode = mode;
    }

    @NotNull
    @Override
    public Class<? extends DBSObject> getPrimaryChildType(@Nullable DBRProgressMonitor monitor) {
        return CloudflareD1Table.class;
    }

    @NotNull
    @Override
    public Collection<? extends GenericTableBase> getChildren(@NotNull DBRProgressMonitor monitor) throws DBException {
        GenericSchema schema = this.getSchema(monitor, "main");
        var tables = schema == null ? null : schema.getTables(monitor);
        return tables == null ? List.of() : tables;
    }

    @Nullable
    @Override
    public DBSObject getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName) throws DBException {
        GenericSchema schema = this.getSchema(monitor, "main");
        return "main".equals(childName) ? schema : (schema == null ? null : schema.getChild(monitor, childName));
    }

    @Override
    public void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope) throws DBException {
        GenericSchema schema = this.getSchema(monitor, "main");
        if (schema != null) {
            schema.cacheStructure(monitor, scope);
        }
    }

    public boolean isInitialDatabase() {
        String databaseId = getDatabaseId();
        return databaseId != null && databaseId.equals(getDataSource().getContainer().getConnectionConfiguration().getDatabaseName());
    }

    public boolean isAppendToScope() {
        return this.appendToScope;
    }

    public void setAppendToScope(boolean appendToScope) {
        this.appendToScope = appendToScope;
    }

    @NotNull
    @Override
    public CloudflareD1Catalog refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (this.isPersisted()) {
            this.setDatabaseDetails(this.readDetails(monitor));
        }
        super.refreshObject(monitor);
        return this;
    }

    @Override
    public boolean isPersisted() {
        return this.persisted;
    }

    @Override
    public void setPersisted(boolean persisted) {
        this.persisted = persisted;
    }

    @NotNull
    private JDBCRemoteInstance instance() {
        return this.database == null ? this.getDataSource().getDefaultInstance() : this.getDataSource().getInstance(this.database);
    }

    @NotNull
    @Override
    public DBCExecutionContext getDefaultContext(@NotNull DBRProgressMonitor monitor, boolean meta) {
        return this.instance().getDefaultContext(monitor, meta);
    }

    @NotNull
    @Override
    public DBCExecutionContext[] getAllContexts() {
        return this.instance().getAllContexts();
    }

    @NotNull
    @Override
    public DBCExecutionContext openIsolatedContext(
        @NotNull DBRProgressMonitor monitor, @NotNull String purpose, @Nullable DBCExecutionContext initFrom
    ) throws DBException {
        return this.instance().openIsolatedContext(monitor, purpose, initFrom);
    }

    @Override
    public void shutdown(@NotNull DBRProgressMonitor monitor) {
        if (this.database != null) {
            this.instance().shutdown(monitor);
        }
    }

    @NotNull
    @Override
    public DBPExclusiveResource getExclusiveLock() {
        return this.instance().getExclusiveLock();
    }
}
