/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.core.Log;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractTriggerColumn;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.ResultSet;

/**
 * OracleTriggerColumn
 */
public class OracleTriggerColumn extends AbstractTriggerColumn
{
    static final Log log = Log.getLog(OracleTriggerColumn.class);

    private OracleTrigger trigger;
    private String name;
    private OracleTableColumn tableColumn;
    private boolean columnList;

    public OracleTriggerColumn(
        DBRProgressMonitor monitor,
        OracleTrigger trigger,
        OracleTableColumn tableColumn,
        ResultSet dbResult) throws DBException
    {
        this.trigger = trigger;
        this.tableColumn = tableColumn;
        this.name = JDBCUtils.safeGetString(dbResult, "COLUMN_NAME");
        this.columnList = JDBCUtils.safeGetBoolean(dbResult, "COLUMN_LIST", "YES");
    }

    OracleTriggerColumn(OracleTrigger trigger, OracleTriggerColumn source)
    {
        this.trigger = trigger;
        this.tableColumn = source.tableColumn;
        this.columnList = source.columnList;
    }

    @Override
    public OracleTrigger getTrigger()
    {
        return trigger;
    }

    @Override
    @Property(viewable = true, order = 1)
    public String getName()
    {
        return name;
    }

    @Override
    @Property(viewable = true, order = 2)
    public OracleTableColumn getTableColumn()
    {
        return tableColumn;
    }

    @Override
    public int getOrdinalPosition()
    {
        return 0;
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return tableColumn.getDescription();
    }

    @Override
    public OracleTrigger getParentObject()
    {
        return trigger;
    }

    @NotNull
    @Override
    public OracleDataSource getDataSource()
    {
        return trigger.getDataSource();
    }

}
