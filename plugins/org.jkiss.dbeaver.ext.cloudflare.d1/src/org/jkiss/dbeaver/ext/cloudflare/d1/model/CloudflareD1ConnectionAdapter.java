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
import org.jkiss.dbeaver.ext.cloudflare.d1.CloudflareD1ConnectionSettings;
import org.jkiss.dbeaver.ext.cloudflare.d1.internal.CloudflareD1ModelMessages;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRBlockingObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class CloudflareD1ConnectionAdapter {

    private CloudflareD1ConnectionAdapter() {
    }

    @NotNull
    public static List<CloudflareD1Database> list(@NotNull DBRProgressMonitor monitor, @NotNull CloudflareD1DataSource source)
        throws DBException {
        return call(monitor, source,
            client -> listDatabases(client, source.getContainer().getActualConnectionConfiguration()));
    }

    @NotNull
    static List<CloudflareD1Database> listDatabases(
        @NotNull CloudflareD1AdminClient client, @NotNull DBPConnectionConfiguration configuration
    ) throws SQLException {
        String scope = configuration.getProperty(CloudflareD1ConnectionSettings.DATABASE_IDS);
        if (scope == null || scope.isBlank()) {
            return client.listDatabases();
        }
        String ids = CloudflareD1ConnectionSettings.serializeScope(configuration.getDatabaseName(), scope);
        List<CloudflareD1Database> databases = new ArrayList<>();
        for (String id : ids.split(",")) {
            databases.add(client.getDatabase(id));
        }
        return List.copyOf(databases);
    }

    @NotNull
    public static List<DatabaseChoice> discoverDatabases(
        @NotNull DBRProgressMonitor monitor, @NotNull DBPConnectionConfiguration configuration
    ) throws DBException {
        return run(monitor, () -> CloudflareD1AdminClient.from(configuration).listDatabases().stream()
            .map(database -> new DatabaseChoice(database.id(), database.name())).toList());
    }

    @NotNull
    public static List<DatabaseChoice> availableDatabases(
        @NotNull DBRProgressMonitor monitor, @NotNull CloudflareD1DataSource source
    ) throws DBException {
        if (monitor.isCanceled()) {
            throw new DBException(CloudflareD1ModelMessages.error_cancelled);
        }
        var catalogs = source.getCatalogs();
        if (catalogs == null) {
            return List.of();
        }
        return catalogs.stream()
            .filter(CloudflareD1Catalog.class::isInstance)
            .map(CloudflareD1Catalog.class::cast)
            .map(catalog -> new DatabaseChoice(catalog.requireDatabaseId(), catalog.getName()))
            .toList();
    }

    @NotNull
    public static CloudflareD1Database details(@NotNull DBRProgressMonitor monitor, @NotNull CloudflareD1Catalog catalog)
        throws DBException {
        return call(monitor, catalog.getDataSource(), client -> client.getDatabase(catalog.requireDatabaseId()));
    }

    @NotNull
    public static CloudflareD1Database create(@NotNull DBRProgressMonitor monitor, @NotNull CloudflareD1Catalog catalog)
        throws DBException {
        return call(monitor, catalog.getDataSource(), client -> client.createDatabase(
            catalog.getName(), placement(catalog.getJurisdiction(), catalog.getPrimaryLocationHint())));
    }

    @NotNull
    static CreateOptions placement(@Nullable String jurisdiction, @Nullable String location) {
        return new CreateOptions(jurisdiction, jurisdiction == null || jurisdiction.isBlank() ? location : null);
    }

    public static void validateDeletion(@NotNull CloudflareD1Catalog catalog) throws DBException {
        if (catalog.isInitialDatabase()) {
            throw new DBException(CloudflareD1ModelMessages.error_delete_initial);
        }
    }

    public static void delete(@NotNull DBRProgressMonitor monitor, @NotNull CloudflareD1Catalog catalog) throws DBException {
        validateDeletion(catalog);
        CloudflareD1DataSource source = catalog.getDataSource();
        String databaseId = catalog.requireDatabaseId();
        source.closeCatalogContexts(monitor, databaseId);
        call(monitor, source, client -> {
            client.deleteDatabase(databaseId);
            return Boolean.TRUE;
        });
        source.removeDatabaseScope(databaseId);
        source.refreshObject(monitor);
    }

    public static void setReplication(
        @NotNull DBRProgressMonitor monitor, @NotNull CloudflareD1Catalog catalog, @NotNull ReplicationMode mode
    ) throws DBException {
        catalog.setDatabaseDetails(call(monitor, catalog.getDataSource(),
            client -> client.setReadReplication(catalog.requireDatabaseId(), mode)));
        DBUtils.fireObjectUpdate(catalog);
    }

    @NotNull
    static String errorMessage(@NotNull SQLException error) {
        String state = error.getSQLState();
        String message = error.getMessage() == null ? "" : error.getMessage();
        if ("HY008".equals(state)) {
            return CloudflareD1ModelMessages.error_cancelled;
        }
        if ("HYT00".equals(state)) {
            return CloudflareD1ModelMessages.error_timeout;
        }
        if (message.contains("HTTP 401")) {
            return CloudflareD1ModelMessages.error_authentication;
        }
        if ("28000".equals(state) || message.contains("HTTP 403")) {
            return CloudflareD1ModelMessages.error_permission;
        }
        if (message.contains("HTTP 404") || "3D000".equals(state)) {
            return CloudflareD1ModelMessages.error_not_found;
        }
        if (message.contains("HTTP 429")) {
            return CloudflareD1ModelMessages.error_rate_limit;
        }
        return CloudflareD1ModelMessages.error_request;
    }

    private static <T> T call(
        @NotNull DBRProgressMonitor monitor, @NotNull CloudflareD1DataSource source, @NotNull DriverAction<T> action
    ) throws DBException {
        return run(monitor, () -> action.execute(CloudflareD1AdminClient.from(
            source.getContainer().getActualConnectionConfiguration())));
    }

    /** Runs an administration operation with cancellation and sanitized API errors. */
    public static <T> T run(@NotNull DBRProgressMonitor monitor, @NotNull AdministrationAction<T> action) throws DBException {
        if (monitor.isCanceled()) {
            throw new DBException(CloudflareD1ModelMessages.error_cancelled);
        }
        final Thread worker = Thread.currentThread();
        final AtomicBoolean active = new AtomicBoolean(true);
        monitor.startBlock(new DBRBlockingObject() {
            @Override
            public void cancelBlock(@NotNull DBRProgressMonitor progress, @NotNull Thread blockThread) {
                if (active.get()) {
                    worker.interrupt();
                }
            }

            @NotNull
            @Override
            public Thread getBlockThread() {
                return worker;
            }
        }, CloudflareD1ModelMessages.task_administration);
        try {
            return action.execute();
        } catch (SQLException e) {
            throw new DBException(errorMessage(e));
        } finally {
            active.set(false);
            monitor.endBlock();
        }
    }

    @FunctionalInterface
    private interface DriverAction<T> {
        T execute(@NotNull CloudflareD1AdminClient client) throws SQLException;
    }

    public record CreateOptions(@Nullable String jurisdiction, @Nullable String primaryLocationHint) {
    }

    public enum ReplicationMode {
        AUTO,
        DISABLED
    }

    @FunctionalInterface
    public interface AdministrationAction<T> {
        T execute() throws SQLException;
    }

    public record DatabaseChoice(@NotNull String id, @NotNull String name) {
    }
}
