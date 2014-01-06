/*
 * Copyright (C) 2010-2013 Serge Rieder
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
package org.jkiss.dbeaver.ext.mssql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableIndex;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;

import java.util.ArrayList;
import java.util.List;

/**
 * MSSQLTableIndex
 */
public class MSSQLTableIndex extends JDBCTableIndex<MSSQLCatalog, MSSQLTable>
{
    private boolean nonUnique;
    private String comment;
    private List<MSSQLTableIndexColumn> columns;

    public MSSQLTableIndex(
        MSSQLTable table,
        DBSIndexType indexType)
    {
        super(table.getContainer(), table, null, indexType, false);
    }

    public MSSQLTableIndex(
        MSSQLTable table,
        boolean nonUnique,
        String indexName,
        DBSIndexType indexType,
        String comment)
    {
        super(table.getContainer(), table, indexName, indexType, true);
        this.nonUnique = nonUnique;
        this.comment = comment;
    }

    /**
     * Copy constructor
     * @param source source index
     */
    MSSQLTableIndex(MSSQLTableIndex source)
    {
        super(source);
        this.nonUnique = source.nonUnique;
        if (source.columns != null) {
            this.columns = new ArrayList<MSSQLTableIndexColumn>(source.columns.size());
            for (MSSQLTableIndexColumn sourceColumn : source.columns) {
                this.columns.add(new MSSQLTableIndexColumn(this, sourceColumn));
            }
        }
    }

    @NotNull
    @Override
    public MSSQLDataSource getDataSource()
    {
        return getTable().getDataSource();
    }

    @Override
    @Property(viewable = true, order = 5)
    public boolean isUnique()
    {
        return !nonUnique;
    }

    @Override
    @Property(viewable = true, order = 6)
    public String getDescription()
    {
        return comment;
    }

    @Override
    public List<MSSQLTableIndexColumn> getAttributeReferences(DBRProgressMonitor monitor)
    {
        return columns;
    }

    public MSSQLTableIndexColumn getColumn(String columnName)
    {
        return DBUtils.findObject(columns, columnName);
    }

    void setColumns(List<MSSQLTableIndexColumn> columns)
    {
        this.columns = columns;
    }

    public void addColumn(MSSQLTableIndexColumn column)
    {
        if (columns == null) {
            columns = new ArrayList<MSSQLTableIndexColumn>();
        }
        columns.add(column);
    }

    @Override
    public String getFullQualifiedName()
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getTable().getContainer(),
            this);
    }
}
