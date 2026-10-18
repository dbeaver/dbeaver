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
package org.jkiss.dbeaver.ext.oracle.model;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Right after connect, fills the caches of schemas which have a metadata snapshot (tables, packages, columns, indexes),
 * so the navigator and SQL completion find them in memory on first use instead of waiting for the snapshot file
 * to be read. Schemas without stored columns read them from the database here, in background.
 * The user's own schema and PUBLIC are always loaded: SQL completion looks into them for every name.
 * Other application schemas get their object names only.
 */
class OracleMetadataWarmupJob extends AbstractJob {

    private static final Log log = Log.getLog(OracleMetadataWarmupJob.class);

    private final OracleDataSource dataSource;

    OracleMetadataWarmupJob(@NotNull OracleDataSource dataSource) {
        super("Restore metadata of " + dataSource.getContainer().getName());
        this.dataSource = dataSource;
        setSystem(true);
        setUser(false);
    }

    @NotNull
    @Override
    protected IStatus run(@NotNull DBRProgressMonitor monitor) {
        try {
            // Schemas SQL completion always looks into: the user's own one and PUBLIC (public synonyms)
            String userName = CommonUtils.notEmpty(dataSource.getContainer().getConnectionConfiguration().getUserName());
            List<OracleSchema> schemas = new ArrayList<>();
            schemas.add(dataSource.getPublicSchema());
            for (OracleSchema schema : dataSource.getSchemas(monitor)) {
                if (schema.getName().equalsIgnoreCase(userName) ||
                    Files.exists(OracleMetadataSnapshot.getSnapshotFile(dataSource.getContainer(), schema.getName()))) {
                    schemas.add(schema);
                }
            }
            for (OracleSchema schema : schemas) {
                if (monitor.isCanceled()) {
                    break;
                }
                schema.getTableCache().getAllObjects(monitor, schema);
                schema.getPackageCache().getAllObjects(monitor, schema);
                OracleMetadataSnapshot snapshot = schema.getMetadataSnapshot();
                if (snapshot != null && snapshot.isColumnsComplete()) {
                    schema.getTableCache().loadChildren(monitor, schema, null);
                } else {
                    schema.getTableCache().prefetchColumns(monitor, schema);
                }
                schema.getIndexCache().getAllObjects(monitor, schema);
            }
            // Schemas never opened before: names only (a light query), so the first completion or navigator
            // expansion does not wait for them. Details are read when a schema is used.
            for (OracleSchema schema : dataSource.getSchemas(monitor)) {
                if (monitor.isCanceled()) {
                    break;
                }
                if (!schema.isSystem() && !schemas.contains(schema)) {
                    schema.getTableCache().prefetchNames(monitor, schema);
                    if (schema.getTableCache().isFullyCached()) {
                        // Table sizes shown by the navigator: one light aggregate query
                        schema.collectObjectStatistics(monitor, false, false);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Error restoring metadata of " + dataSource.getContainer().getName() + ": " + e.getMessage());
        }
        return Status.OK_STATUS;
    }
}
