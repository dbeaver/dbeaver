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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericCatalog;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

/**
 * A Frostlake schema: everything a generic schema is, plus the Snowflake object kinds that JDBC
 * metadata has no call for.
 *
 * <p>Each kind gets its own lazily-built cache, so the navigator loads only the folder that was
 * expanded. The getters below are what the {@code property=} attributes in plugin.xml resolve
 * against — {@code property="stages"} calls {@link #getStages}.
 *
 * <p>Sequences are absent from this list on purpose: the generic model already declares
 * {@code getSequences} and lists them from JDBC metadata, so the Sequences folder is served by the
 * generic implementation rather than by a SHOW.
 */
public class FrostlakeSchema extends GenericSchema {

    private final Map<FrostlakeObjectKind, FrostlakeObjectCache> caches =
        new EnumMap<>(FrostlakeObjectKind.class);

    public FrostlakeSchema(@NotNull GenericDataSource dataSource,
                           @Nullable GenericCatalog catalog,
                           @NotNull String schemaName) {
        super(dataSource, catalog, schemaName);
    }

    /** The cache for one kind, created on first use. */
    @NotNull
    private synchronized FrostlakeObjectCache cacheOf(@NotNull FrostlakeObjectKind kind) {
        FrostlakeObjectCache cache = caches.get(kind);
        if (cache == null) {
            cache = new FrostlakeObjectCache(kind);
            caches.put(kind, cache);
        }
        return cache;
    }

    @NotNull
    private Collection<FrostlakeObject> objects(@NotNull DBRProgressMonitor monitor,
                                                @NotNull FrostlakeObjectKind kind) throws DBException {
        return cacheOf(kind).getAllObjects(monitor, this);
    }

    @NotNull
    public Collection<FrostlakeObject> getStages(@NotNull DBRProgressMonitor monitor) throws DBException {
        return objects(monitor, FrostlakeObjectKind.STAGE);
    }

    @NotNull
    public Collection<FrostlakeObject> getPipes(@NotNull DBRProgressMonitor monitor) throws DBException {
        return objects(monitor, FrostlakeObjectKind.PIPE);
    }

    @NotNull
    public Collection<FrostlakeObject> getStreams(@NotNull DBRProgressMonitor monitor) throws DBException {
        return objects(monitor, FrostlakeObjectKind.STREAM);
    }

    @NotNull
    public Collection<FrostlakeObject> getTasks(@NotNull DBRProgressMonitor monitor) throws DBException {
        return objects(monitor, FrostlakeObjectKind.TASK);
    }

    @NotNull
    public Collection<FrostlakeObject> getFileFormats(@NotNull DBRProgressMonitor monitor) throws DBException {
        return objects(monitor, FrostlakeObjectKind.FILE_FORMAT);
    }

    @NotNull
    public Collection<FrostlakeObject> getDynamicTables(@NotNull DBRProgressMonitor monitor) throws DBException {
        return objects(monitor, FrostlakeObjectKind.DYNAMIC_TABLE);
    }

    @NotNull
    public Collection<FrostlakeObject> getTags(@NotNull DBRProgressMonitor monitor) throws DBException {
        return objects(monitor, FrostlakeObjectKind.TAG);
    }

    @NotNull
    public Collection<FrostlakeObject> getMaskingPolicies(@NotNull DBRProgressMonitor monitor) throws DBException {
        return objects(monitor, FrostlakeObjectKind.MASKING_POLICY);
    }

    @NotNull
    public Collection<FrostlakeObject> getRowAccessPolicies(@NotNull DBRProgressMonitor monitor) throws DBException {
        return objects(monitor, FrostlakeObjectKind.ROW_ACCESS_POLICY);
    }

    @NotNull
    public Collection<FrostlakeObject> getCortexSearchServices(@NotNull DBRProgressMonitor monitor) throws DBException {
        return objects(monitor, FrostlakeObjectKind.CORTEX_SEARCH_SERVICE);
    }

    /** Refreshing the schema drops every kind's cache along with the generic content. */
    @Nullable
    @Override
    public synchronized DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        for (FrostlakeObjectCache cache : caches.values()) {
            cache.clearCache();
        }
        return super.refreshObject(monitor);
    }
}
