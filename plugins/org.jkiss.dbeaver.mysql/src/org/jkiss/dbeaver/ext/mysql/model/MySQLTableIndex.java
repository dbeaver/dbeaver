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
package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableIndex;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;

import java.util.ArrayList;
import java.util.List;

/**
 * MySQLTableIndex
 */
public class MySQLTableIndex extends JDBCTableIndex<MySQLCatalog, MySQLTable>
{
    private boolean nonUnique;
    private String comment;
    private List<MySQLTableIndexColumn> columns;

    public MySQLTableIndex(
        MySQLTable table,
        DBSIndexType indexType)
    {
        super(table.getContainer(), table, null, indexType, false);
    }

    public MySQLTableIndex(
        MySQLTable table,
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
    MySQLTableIndex(MySQLTableIndex source)
    {
        super(source);
        this.nonUnique = source.nonUnique;
        if (source.columns != null) {
            this.columns = new ArrayList<MySQLTableIndexColumn>(source.columns.size());
            for (MySQLTableIndexColumn sourceColumn : source.columns) {
                this.columns.add(new MySQLTableIndexColumn(this, sourceColumn));
            }
        }
    }

    @NotNull
    @Override
    public MySQLDataSource getDataSource()
    {
        return getTable().getDataSource();
    }

    @Override
    @Property(viewable = true, order = 5)
    public boolean isUnique()
    {
        return !nonUnique;
    }

    @Nullable
    @Override
    @Property(viewable = true, order = 6)
    public String getDescription()
    {
        return comment;
    }

    @Override
    public List<MySQLTableIndexColumn> getAttributeReferences(DBRProgressMonitor monitor)
    {
        return columns;
    }

    public MySQLTableIndexColumn getColumn(String columnName)
    {
        return DBUtils.findObject(columns, columnName);
    }

    void setColumns(List<MySQLTableIndexColumn> columns)
    {
        this.columns = columns;
    }

    public void addColumn(MySQLTableIndexColumn column)
    {
        if (columns == null) {
            columns = new ArrayList<MySQLTableIndexColumn>();
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
