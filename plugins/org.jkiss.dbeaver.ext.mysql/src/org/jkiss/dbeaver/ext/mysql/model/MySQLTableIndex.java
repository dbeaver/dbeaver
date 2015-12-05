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
package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableIndex;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * MySQLTableIndex
 */
public class MySQLTableIndex extends JDBCTableIndex<MySQLCatalog, MySQLTable>
{
    private boolean nonUnique;
    private String additionalInfo;
    private String indexComment;
    private long cardinality;
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
        this.indexComment = comment;
    }

    /**
     * Copy constructor
     * @param source source index
     */
    MySQLTableIndex(MySQLTableIndex source)
    {
        super(source);
        this.nonUnique = source.nonUnique;
        this.cardinality = source.cardinality;
        this.indexComment = source.indexComment;
        this.additionalInfo = source.additionalInfo;
        if (source.columns != null) {
            this.columns = new ArrayList<>(source.columns.size());
            for (MySQLTableIndexColumn sourceColumn : source.columns) {
                this.columns.add(new MySQLTableIndexColumn(this, sourceColumn));
            }
        }
    }

    public MySQLTableIndex(MySQLTable parent, String indexName, DBSIndexType indexType, ResultSet dbResult) {
        super(
            parent.getContainer(),
            parent,
            indexName,
            indexType,
            true);
        this.nonUnique = JDBCUtils.safeGetInt(dbResult, MySQLConstants.COL_NON_UNIQUE) != 0;
        this.cardinality = JDBCUtils.safeGetLong(dbResult, "cardinality");
        this.indexComment = JDBCUtils.safeGetString(dbResult, "INDEX_COMMENT");
        this.additionalInfo = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_COMMENT);
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
    @Property(viewable = true, order = 100)
    public String getDescription()
    {
        return indexComment;
    }

    @Property(viewable = true, order = 20)
    public long getCardinality() {
        return cardinality;
    }

    @Property(viewable = false, order = 30)
    public String getAdditionalInfo() {
        return additionalInfo;
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
            columns = new ArrayList<>();
        }
        columns.add(column);
    }

    @NotNull
    @Override
    public String getFullQualifiedName()
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getTable().getContainer(),
            this);
    }
}
