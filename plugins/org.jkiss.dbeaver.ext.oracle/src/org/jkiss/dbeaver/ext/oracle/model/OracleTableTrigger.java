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
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.ResultSet;
import java.util.Collection;
import java.util.List;

/**
 * OracleTableTrigger
 */
public class OracleTableTrigger extends OracleTrigger<OracleTableBase>
{
    private List<OracleTriggerColumn> columns;

    public OracleTableTrigger(OracleTableBase table, String name)
    {
        super(table, name);
    }

    public OracleTableTrigger(
        OracleTableBase table,
        ResultSet dbResult)
    {
        super(table, dbResult);
    }

    @Override
    @Property(viewable = true, order = 4)
    public OracleTableBase getTable()
    {
        return parent;
    }

    @Override
    public OracleSchema getSchema() {
        return parent.getSchema();
    }

    @Association
    public Collection<OracleTriggerColumn> getColumns(DBRProgressMonitor monitor) throws DBException
    {
        if (columns == null) {
            parent.triggerCache.loadChildren(monitor, parent, this);
        }
        return columns;
    }

    boolean isColumnsCached()
    {
        return columns != null;
    }

    void setColumns(List<OracleTriggerColumn> columns)
    {
        this.columns = columns;
    }


}
