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
package org.jkiss.dbeaver.ext.starrocks.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;

import java.util.Collections;
import java.util.List;

/**
 * StarRocks Database - represents a database/schema within a StarRocks catalog.
 */
public class StarRocksDatabase extends GenericSchema {

    public StarRocksDatabase(
        @NotNull StarRocksDataSource dataSource,
        @Nullable StarRocksCatalog catalog,
        @NotNull String schemaName
    ) {
        super(dataSource, catalog, schemaName);
    }

    @NotNull
    @Override
    public StarRocksDataSource getDataSource() {
        return (StarRocksDataSource) super.getDataSource();
    }

    @Nullable
    @Override
    public StarRocksCatalog getCatalog() {
        return (StarRocksCatalog) super.getCatalog();
    }

    @NotNull
    @Override
    public Class<? extends DBSEntity> getPrimaryChildType(@Nullable DBRProgressMonitor monitor) throws DBException {
        return GenericTableBase.class;
    }

    @NotNull
    public List<StarRocksTable> getTables(@NotNull DBRProgressMonitor monitor) throws DBException {
        return getTableCache().getTypedObjects(monitor, this, StarRocksTable.class);
    }

    @NotNull
    public List<StarRocksView> getViews(@NotNull DBRProgressMonitor monitor) throws DBException {
        return getTableCache().getTypedObjects(monitor, this, StarRocksView.class);
    }

    /**
     * Returns only materialized views (excludes tables and regular views).
     *
     * Note: Materialized views are only supported in internal catalogs.
     */
    @NotNull
    public List<StarRocksMaterializedView> getMaterializedViews(@NotNull DBRProgressMonitor monitor) throws DBException {
        StarRocksCatalog catalog = getCatalog();
        if (catalog != null && !catalog.isInternal()) {
            // External catalogs don't support materialized views
            return Collections.emptyList();
        }
        return getTableCache().getTypedObjects(monitor, this, StarRocksMaterializedView.class);
    }
}
