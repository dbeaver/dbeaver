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
import org.jkiss.dbeaver.ext.generic.model.GenericCatalog;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Collection;

/**
 * StarRocks Catalog - represents a catalog (e.g., default_catalog, hive_catalog)
 * Contains multiple databases (schemas)
 */
public class StarRocksCatalog extends GenericCatalog {

    private String type;
    private String comment;

    public StarRocksCatalog(@NotNull StarRocksDataSource dataSource, @NotNull String catalogName) {
        super(dataSource, catalogName);
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @Property(viewable = true, order = 2)
    public String getType() {
        return type;
    }

    @Property(viewable = true, order = 3)
    public String getComment() {
        return comment;
    }

    @Nullable
    @Override
    public String getDescription() {
        return comment;
    }

    public boolean isInternal() {
        return "Internal".equalsIgnoreCase(type); //$NON-NLS-1$
    }

    @NotNull
    @Override
    public StarRocksDataSource getDataSource() {
        return (StarRocksDataSource) super.getDataSource();
    }

    @Nullable
    public StarRocksDatabase getDatabase(@NotNull DBRProgressMonitor monitor, @NotNull String name) throws DBException {
        return (StarRocksDatabase) getSchema(monitor, name);
    }

    @Nullable
    public StarRocksDatabase getCachedDatabase(@NotNull String name) {
        try {
            Collection<GenericSchema> schemas = getSchemas(null);
            if (schemas != null) {
                for (GenericSchema schema : schemas) {
                    if (schema.getName().equals(name)) {
                        return (StarRocksDatabase) schema;
                    }
                }
            }
        } catch (DBException e) {
            // Return null if schemas haven't been loaded yet
        }
        return null;
    }

    @NotNull
    @Override
    public Class<? extends DBSObject> getPrimaryChildType(@Nullable DBRProgressMonitor monitor) throws DBException {
        return StarRocksDatabase.class;
    }
}
