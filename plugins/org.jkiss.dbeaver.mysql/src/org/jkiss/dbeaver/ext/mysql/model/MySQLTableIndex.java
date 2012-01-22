/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableIndex;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSIndexType;

import java.util.ArrayList;
import java.util.List;

/**
 * MySQLTableIndex
 */
public class MySQLTableIndex extends JDBCTableIndex<MySQLTable>
{
    private boolean nonUnique;
    private String comment;
    private List<MySQLTableIndexColumn> columns;

    public MySQLTableIndex(
        MySQLTable table,
        DBSIndexType indexType)
    {
        super(table, null, indexType, false);
    }

    public MySQLTableIndex(
        MySQLTable table,
        boolean nonUnique,
        String indexName,
        DBSIndexType indexType,
        String comment)
    {
        super(table, indexName, indexType, true);
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

    public MySQLDataSource getDataSource()
    {
        return getTable().getDataSource();
    }

    @Property(name = "Unique", viewable = true, order = 5)
    public boolean isUnique()
    {
        return !nonUnique;
    }

    @Property(name = "Comment", viewable = true, order = 6)
    public String getDescription()
    {
        return comment;
    }

    public List<MySQLTableIndexColumn> getColumns(DBRProgressMonitor monitor)
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

    public String getFullQualifiedName()
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getTable().getContainer(),
            this);
    }
}
