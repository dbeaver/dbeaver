/*
 * Copyright (C) 2013      Denis Forveille titou10.titou10@gmail.com
 * Copyright (C) 2010-2013 Serge Rieder serge@jkiss.org
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
package org.jkiss.dbeaver.ext.db2.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.DB2Constants;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2IndexColOrder;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractTableIndexColumn;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;

/**
 * DB2 Index Column
 * 
 * @author Denis Forveille
 */
public class DB2IndexColumn extends AbstractTableIndexColumn {
    private DB2Index db2Index;
    private DB2TableColumn tableColumn;

    private Integer colSeq;
    private DB2IndexColOrder colOrder;
    private String collationSchema;
    private String collationNane;

    // -----------------
    // Constructors
    // -----------------

    public DB2IndexColumn(DBRProgressMonitor monitor, DB2Index db2Index, ResultSet dbResult) throws DBException
    {

        DB2DataSource db2DataSource = (DB2DataSource) db2Index.getDataSource();

        this.db2Index = db2Index;
        this.colSeq = JDBCUtils.safeGetInteger(dbResult, "COLSEQ");
        this.colOrder = CommonUtils.valueOf(DB2IndexColOrder.class, JDBCUtils.safeGetString(dbResult, "COLORDER"));

        if (db2DataSource.isAtLeastV9_5()) {
            this.collationSchema = JDBCUtils.safeGetStringTrimmed(dbResult, "COLLATIONSCHEMA");
            this.collationNane = JDBCUtils.safeGetString(dbResult, "COLLATIONNAME");
        }

        // Look for Table Column
        DB2Table db2Table = db2Index.getTable();
        String columnName = JDBCUtils.safeGetString(dbResult, "COLNAME");
        this.tableColumn = db2Table.getAttribute(monitor, columnName);
        if (tableColumn == null) {
            StringBuilder sb = new StringBuilder(64);
            sb.append("Column '");
            sb.append(columnName);
            sb.append("' not found in table '");
            sb.append(db2Table.getName());
            sb.append("' for index '");
            sb.append(db2Index.getName());
            sb.append("'");
            throw new DBException(sb.toString());
        }
    }

    public DB2IndexColumn(DB2Index db2Index, DB2TableColumn tableColumn, int ordinalPosition)
    {
        this.db2Index = db2Index;
        this.tableColumn = tableColumn;
        this.colSeq = ordinalPosition;
        this.colOrder = DB2IndexColOrder.A; // Force Ascending ..
    }

    // -----------------
    // Business Contract
    // -----------------
    @NotNull
    @Override
    public DB2DataSource getDataSource()
    {
        return db2Index.getDataSource();
    }

    @Override
    public DB2Index getParentObject()
    {
        return db2Index;
    }

    @Override
    public DB2Index getIndex()
    {
        return db2Index;
    }

    @Override
    public String getDescription()
    {
        return tableColumn.getDescription();
    }

    @Override
    public int getOrdinalPosition()
    {
        return colSeq;
    }

    @Override
    public boolean isAscending()
    {
        return colOrder.isAscending();
    }

    @Override
    public String getName()
    {
        return tableColumn.getName();
    }

    // -----------------
    // Properties
    // -----------------

    @Override
    // col name in index name
    @Property(viewable = true, order = 1, id = "name")
    public DB2TableColumn getTableColumn()
    {
        return tableColumn;
    }

    // order in index schema name
    @Property(viewable = true, editable = false, order = 2, id = "indSchema")
    public Integer getColSeq()
    {
        return colSeq;
    }

    @Property(viewable = true, editable = true, order = 3, id = "table")
    public DB2IndexColOrder getColOrder()
    {
        return colOrder;
    }

    @Property(viewable = false, editable = false, category = DB2Constants.CAT_COLLATION)
    public String getCollationSchema()
    {
        return collationSchema;
    }

    @Property(viewable = false, editable = false, category = DB2Constants.CAT_COLLATION)
    public String getCollationNane()
    {
        return collationNane;
    }
}
