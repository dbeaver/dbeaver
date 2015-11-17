/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.impl.struct.AbstractTrigger;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * GenericProcedure
 */
public class GenericTrigger extends AbstractTrigger implements GenericStoredCode
{
    static final Log log = Log.getLog(GenericTrigger.class);

    private GenericTable table;
    private String source;

    public GenericTrigger(GenericTable table, String name, String description)
    {
        super(name, description);
        this.table = table;
    }

    @Override
    @Property(viewable = true, order = 4)
    public GenericTable getTable()
    {
        return table;
    }

    @Override
    public GenericTable getParentObject()
    {
        return table;
    }

    @NotNull
    @Override
    public GenericDataSource getDataSource()
    {
        return table.getDataSource();
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getSource(DBRProgressMonitor monitor) throws DBException
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
