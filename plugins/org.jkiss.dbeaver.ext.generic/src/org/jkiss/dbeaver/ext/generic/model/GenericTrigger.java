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
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSTrigger;

/**
 * GenericProcedure
 */
public class GenericTrigger implements DBSTrigger, GenericScriptObject
{
    @NotNull
    private final GenericStructContainer container;
    @Nullable
    private final GenericTable table;
    private String name;
    private String description;
    protected String source;

    public GenericTrigger(@NotNull GenericStructContainer container, @Nullable GenericTable table, String name, String description) {
        this.container = container;
        this.table = table;
        this.name = name;
        this.description = description;
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = true, order = 1)
    public String getName() {
        return name;
    }

    @Nullable
    @Override
    @Property(viewable = true, order = 100)
    public String getDescription()
    {
        return description;
    }

    protected void setDescription(String description)
    {
        this.description = description;
    }

    @Override
    public boolean isPersisted()
    {
        return true;
    }

    @Nullable
    @Override
    @Property(viewable = true, order = 4)
    public GenericTable getTable()
    {
        return table;
    }

    @NotNull
    public GenericStructContainer getContainer() {
        return container;
    }

    @Override
    public DBSObject getParentObject()
    {
        return table == null ? container : table;
    }

    @NotNull
    @Override
    public GenericDataSource getDataSource()
    {
        return container.getDataSource();
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(DBRProgressMonitor monitor) throws DBException
    {
        if (source == null) {
            source = getDataSource().getMetaModel().getTriggerDDL(monitor, this);
        }
        return source;
    }

    public void setSource(String sourceText) throws DBException
    {
        source = sourceText;
    }

}
