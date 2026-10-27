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
package org.jkiss.dbeaver.ext.cloudflare.d1.edit;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.cloudflare.d1.internal.CloudflareD1ModelMessages;
import org.jkiss.dbeaver.ext.cloudflare.d1.model.CloudflareD1Catalog;
import org.jkiss.dbeaver.ext.cloudflare.d1.model.CloudflareD1ConnectionAdapter;
import org.jkiss.dbeaver.ext.cloudflare.d1.model.CloudflareD1DataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericCatalog;
import org.jkiss.dbeaver.ext.generic.model.GenericExecutionContext;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.edit.DirectDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

import java.util.List;
import java.util.Map;

public class CloudflareD1CatalogManager extends SQLObjectEditor<CloudflareD1Catalog, CloudflareD1DataSource> {
    public static final String CONFIRM_DELETE = "d1.confirmDelete";
    public static final String CREATED = "d1.created";

    @Override
    public boolean canEditObject(@NotNull CloudflareD1Catalog object) {
        return !object.getDataSource().getContainer().isConnectionReadOnly() && super.canEditObject(object);
    }

    @Override
    protected void addObjectModifyActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectChangeCommand command,
        @NotNull Map<String, Object> options
    ) throws DBException {
        if (!command.hasProperty("readReplicationMode")) {
            return;
        }
        CloudflareD1Catalog catalog = command.getObject();
        String selectedMode = catalog.getReadReplicationMode();
        if (selectedMode == null) {
            throw new DBException(CloudflareD1ModelMessages.error_replication_required);
        }
        CloudflareD1ConnectionAdapter.ReplicationMode mode = CloudflareD1ConnectionAdapter.ReplicationMode.valueOf(selectedMode);
        actions.add(new DirectDatabasePersistAction(CloudflareD1ModelMessages.task_replication) {
            @Override
            public void afterExecute(@NotNull DBCSession session, @Nullable Throwable error) throws DBCException {
                if (error != null) {
                    return;
                }
                try {
                    CloudflareD1ConnectionAdapter.setReplication(monitor, catalog, mode);
                } catch (DBException e) {
                    throw new DBCException(e, executionContext);
                }
            }
        });
    }

    @Override
    public boolean canDeleteObject(@NotNull CloudflareD1Catalog object) {
        return !object.isInitialDatabase() && super.canDeleteObject(object);
    }

    @Override
    public void deleteObject(
        @NotNull DBECommandContext context, @NotNull CloudflareD1Catalog catalog, @NotNull Map<String, Object> options
    ) throws DBException {
        CloudflareD1ConnectionAdapter.validateDeletion(catalog);
        if (configureObject(new VoidProgressMonitor(), context, catalog.getDataSource(), catalog, Map.of(CONFIRM_DELETE, true)) != null) {
            super.deleteObject(context, catalog, options);
        }
    }

    @Override
    public long getMakerOptions(@NotNull DBPDataSource dataSource) {
        return FEATURE_SAVE_IMMEDIATELY;
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, CloudflareD1Catalog> getObjectsCache(@NotNull CloudflareD1Catalog object) {
        return null;
    }

    @NotNull
    @Override
    protected CloudflareD1Catalog createDatabaseObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBECommandContext context,
        @NotNull Object container,
        @Nullable Object copyFrom,
        @NotNull Map<String, Object> options
    ) {
        return new CloudflareD1Catalog((CloudflareD1DataSource) container, "");
    }

    @Override
    protected void addObjectCreateActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectCreateCommand command,
        @NotNull Map<String, Object> options
    ) throws DBException {
        if (command.getObject().getName().isBlank()) {
            throw new DBException("Enter a name for the new Cloudflare D1 database.");
        }
        actions.add(new DirectDatabasePersistAction(CloudflareD1ModelMessages.task_create) {
            @Override
            public void afterExecute(@NotNull DBCSession session, @Nullable Throwable error) throws DBCException {
                if (error != null) {
                    return;
                }
                CloudflareD1Catalog catalog = command.getObject();
                try {
                    catalog.setDatabase(CloudflareD1ConnectionAdapter.create(monitor, catalog));
                    CloudflareD1CatalogManager.this.configureObject(monitor, null, catalog.getDataSource(), catalog, Map.of(CREATED, true));
                    if (catalog.isAppendToScope()) {
                        catalog.getDataSource().appendDatabaseScope(monitor, catalog.requireDatabaseId());
                    }
                    catalog.getDataSource().refreshObject(monitor);
                    GenericCatalog created = catalog.getDataSource().getCatalog(catalog.getName());
                    JDBCExecutionContext context = catalog.getDataSource().getDefaultInstance().getDefaultContext(monitor, false);
                    if (created != null && context instanceof GenericExecutionContext d1) {
                        d1.setDefaultCatalog(monitor, created, null);
                    }
                } catch (DBException e) {
                    throw new DBCException(e, executionContext);
                }
            }
        });
    }

    @Override
    protected void addObjectDeleteActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectDeleteCommand command,
        @NotNull Map<String, Object> options
    ) {
        actions.add(new DirectDatabasePersistAction(CloudflareD1ModelMessages.task_delete) {
            @Override
            public void afterExecute(@NotNull DBCSession session, @Nullable Throwable error) throws DBCException {
                if (error != null) {
                    return;
                }
                CloudflareD1Catalog catalog = command.getObject();
                try {
                    CloudflareD1ConnectionAdapter.delete(monitor, catalog);
                } catch (DBException e) {
                    throw new DBCException(e, executionContext);
                }
            }
        });
    }
}
