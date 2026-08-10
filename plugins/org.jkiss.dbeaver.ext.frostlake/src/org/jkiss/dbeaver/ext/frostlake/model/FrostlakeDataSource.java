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
package org.jkiss.dbeaver.ext.frostlake.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

/**
 * Frostlake data source. Everything the generic model handles stays with the generic model; the
 * Frostlake-specific parts are the schema class, which adds the schema-scoped SHOW-backed kinds, and
 * the account-level kinds below — warehouses, roles and users belong to no schema, so they hang off
 * the data source and appear as siblings of Databases in the tree.
 */
public class FrostlakeDataSource extends GenericDataSource {

    public FrostlakeDataSource(@NotNull DBRProgressMonitor monitor,
                               @NotNull DBPDataSourceContainer container,
                               @NotNull GenericMetaModel metaModel)
        throws DBException {
        super(monitor, container, metaModel, new FrostlakeSQLDialect());
    }

    private final Map<FrostlakeObjectKind, FrostlakeAccountObjectCache> accountCaches =
        new EnumMap<>(FrostlakeObjectKind.class);

    /** The cache for one account-level kind, created on first use. */
    @NotNull
    private synchronized FrostlakeAccountObjectCache cacheOf(@NotNull FrostlakeObjectKind kind) {
        FrostlakeAccountObjectCache cache = accountCaches.get(kind);
        if (cache == null) {
            cache = new FrostlakeAccountObjectCache(kind);
            accountCaches.put(kind, cache);
        }
        return cache;
    }

    @NotNull
    private Collection<FrostlakeObject> accountObjects(@NotNull DBRProgressMonitor monitor,
                                                       @NotNull FrostlakeObjectKind kind) throws DBException {
        return cacheOf(kind).getAllObjects(monitor, this);
    }

    public Collection<FrostlakeObject> getWarehouses(DBRProgressMonitor monitor) throws DBException {
        return accountObjects(monitor, FrostlakeObjectKind.WAREHOUSE);
    }

    public Collection<FrostlakeObject> getRoles(DBRProgressMonitor monitor) throws DBException {
        return accountObjects(monitor, FrostlakeObjectKind.ROLE);
    }

    public Collection<FrostlakeObject> getUsers(DBRProgressMonitor monitor) throws DBException {
        return accountObjects(monitor, FrostlakeObjectKind.USER);
    }

    /** Refreshing the connection drops the account-level caches along with the generic content. */
    @Override
    public synchronized DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        for (FrostlakeAccountObjectCache cache : accountCaches.values()) {
            cache.clearCache();
        }
        return super.refreshObject(monitor);
    }

    @Override
    public boolean splitProceduresAndFunctions() {
        // Frostlake answers getProcedures() and getFunctions() separately, so the navigator shows
        // Procedures and Functions as distinct folders rather than one merged list.
        return true;
    }
}
