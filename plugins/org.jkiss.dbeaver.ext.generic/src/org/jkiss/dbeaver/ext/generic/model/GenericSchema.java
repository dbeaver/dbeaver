/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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
