/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractIndex;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSIndexType;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.ArrayList;
import java.util.List;

/**
 * GenericTable
 */
public class MySQLIndex extends AbstractIndex
{
    private MySQLTable table;
    private boolean nonUnique;
    private String qualifier;
    private String indexName;
    private DBSIndexType indexType;
    private List<MySQLIndexColumn> columns;

    public MySQLIndex(
        MySQLTable table,
        boolean nonUnique,
        String qualifier,
        String indexName,
        DBSIndexType indexType)
    {
        this.table = table;
        this.nonUnique = nonUnique;
        this.qualifier = qualifier;
        this.indexName = indexName;
        this.indexType = indexType;
    }

    /**
     * Copy constructor
     * @param source source index
     */
    MySQLIndex(MySQLIndex source)
    {
        this.table = source.table;
        this.nonUnique = source.nonUnique;
        this.qualifier = source.qualifier;
        this.indexName = source.indexName;
        this.indexType = source.indexType;
        if (source.columns != null) {
            this.columns = new ArrayList<MySQLIndexColumn>(source.columns.size());
            for (MySQLIndexColumn sourceColumn : source.columns) {
                this.columns.add(new MySQLIndexColumn(this, sourceColumn));
            }
        }
    }

    public MySQLDataSource getDataSource()
    {
        return table.getDataSource();
    }

    @Property(name = "Table", viewable = true, order = 2)
    public MySQLTable getTable()
    {
        return table;
    }

    @Property(name = "Unique", viewable = true, order = 5)
    public boolean isUnique()
    {
        return !nonUnique;
    }

    @Property(name = "Index Type", viewable = true, order = 3)
    public DBSIndexType getIndexType()
    {
        return this.indexType;
    }

    @Property(name = "Index Name", viewable = true, order = 1)
    public String getName()
    {
        return indexName;
    }

    @Property(name = "Index Description", viewable = true, order = 6)
    public String getDescription()
    {
        return null;
    }

    public DBSObject getParentObject()
    {
        return table;
    }

    @Property(name = "Qualifier", viewable = true, order = 4)
    public String getQualifier()
    {
        return qualifier;
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

    void addColumn(MySQLIndexColumn column)
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
            getTable(),
            this);
    }
}
