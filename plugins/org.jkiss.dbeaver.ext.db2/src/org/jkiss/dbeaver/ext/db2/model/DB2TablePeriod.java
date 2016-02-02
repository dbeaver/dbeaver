/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2016 Denis Forveille (titou10.titou10@gmail.com)
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
package org.jkiss.dbeaver.ext.db2.model;

import java.sql.ResultSet;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2PeriodType;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.utils.CommonUtils;

/**
 * DB2 Table Period
 * 
 * @author Denis Forveille
 */
public class DB2TablePeriod extends DB2Object<DB2Table> {

    private DB2TableColumn beginColumn;
    private DB2TableColumn endColumn;
    private DB2PeriodType type;
    private DB2Schema historyTableSchema;
    private DB2Table historyTable;

    // -----------------------
    // Constructors
    // -----------------------

    public DB2TablePeriod(DB2Table db2Table, ResultSet dbResult) throws DBException
    {
        super(db2Table, JDBCUtils.safeGetString(dbResult, "PERIODNAME"), true);

        DB2DataSource db2DataSource = db2Table.getDataSource();

        String beginColumnName = JDBCUtils.safeGetString(dbResult, "BEGINCOLNAME");
        String endColumnName = JDBCUtils.safeGetString(dbResult, "ENDCOLNAME");
        String historyTabSchemaName = JDBCUtils.safeGetString(dbResult, "HISTORYTABSCHEMA");
        String historyTabName = JDBCUtils.safeGetString(dbResult, "HISTORYTABNAME");

        this.type = CommonUtils.valueOf(DB2PeriodType.class, JDBCUtils.safeGetString(dbResult, "PERIODTYPE"));

        // Lookup related objects
        VoidProgressMonitor vpm = VoidProgressMonitor.INSTANCE;
        beginColumn = db2Table.getAttribute(vpm, beginColumnName);
        endColumn = db2Table.getAttribute(vpm, endColumnName);
        historyTableSchema = db2DataSource.getSchema(vpm, historyTabSchemaName.trim());
        historyTable = historyTableSchema.getTable(vpm, historyTabName);
    }

    // -----------------
    // Properties
    // -----------------

    @NotNull
    @Override
    @Property(viewable = true, editable = false, order = 1)
    public String getName()
    {
        return super.getName();
    }

    @Property(viewable = true, order = 2)
    public DB2Table getTable()
    {
        return parent;
    }

    @Property(viewable = true, order = 3)
    public DB2PeriodType getType()
    {
        return type;
    }

    @Property(viewable = true, order = 10)
    public DB2TableColumn getBeginColumn()
    {
        return beginColumn;
    }

    @Property(viewable = true, order = 11)
    public DB2TableColumn getEndColumn()
    {
        return endColumn;
    }

    @Property(viewable = true, order = 30)
    public DB2Schema getHistoryTableSchema()
    {
        return historyTableSchema;
    }

    @Property(viewable = true, order = 31)
    public DB2Table getHistoryTable()
    {
        return historyTable;
    }

}
