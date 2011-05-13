/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCIndex;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSIndexType;

import java.util.ArrayList;
import java.util.List;

/**
 * GenericTable
 */
public class MySQLIndex extends JDBCIndex<MySQLTable>
{
    private boolean nonUnique;
    private String comment;
    private List<MySQLIndexColumn> columns;

    public MySQLIndex(
        MySQLTable table,
        DBSIndexType indexType)
    {
        super(table, null, indexType, false);
    }

    public MySQLIndex(
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
    MySQLIndex(MySQLIndex source)
    {
        super(source);
        this.nonUnique = source.nonUnique;
        if (source.columns != null) {
            this.columns = new ArrayList<MySQLIndexColumn>(source.columns.size());
            for (MySQLIndexColumn sourceColumn : source.columns) {
                this.columns.add(new MySQLIndexColumn(this, sourceColumn));
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

    public List<MySQLIndexColumn> getColumns(DBRProgressMonitor monitor)
    {
        return columns;
    }

    public MySQLIndexColumn getColumn(String columnName)
    {
        return DBUtils.findObject(columns, columnName);
    }

    void setColumns(List<MySQLIndexColumn> columns)
    {
        this.columns = columns;
    }

    public void addColumn(MySQLIndexColumn column)
    {
        if (columns == null) {
            columns = new ArrayList<MySQLIndexColumn>();
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
