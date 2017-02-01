/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;

/**
 * GenericSchema
 */
public class GenericSchema extends GenericObjectContainer implements DBSSchema
{
    private GenericCatalog catalog;
    private String schemaName;

    public GenericSchema(GenericDataSource dataSource, String schemaName)
    {
        super(dataSource);
        this.schemaName = schemaName;
    }

    public GenericSchema(GenericCatalog catalog, String schemaName)
    {
        super(catalog.getDataSource());
        this.catalog = catalog;
        this.schemaName = schemaName;
    }

    @Override
    @Property(order = 2)
    public GenericCatalog getCatalog()
    {
        return catalog;
    }

    @Override
    public GenericSchema getSchema()
    {
        return this;
    }

    @Override
    public GenericSchema getObject()
    {
        return this;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName()
    {
        return schemaName;
    }

    @Nullable
    @Override
    @Property(viewable = true, order = 100)
    public String getDescription()
    {
        return null;
    }

    @Override
    public DBSObject getParentObject()
    {
        return catalog != null ? catalog : getDataSource().getContainer();
    }

    @Override
    public Class<? extends DBSEntity> getChildType(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return GenericTable.class;
    }
}
