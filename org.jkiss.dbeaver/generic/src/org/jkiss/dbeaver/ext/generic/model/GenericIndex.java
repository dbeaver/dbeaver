/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.struct.AbstractIndex;
import org.jkiss.dbeaver.model.struct.DBSIndexType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.ArrayList;
import java.util.List;

/**
 * GenericTable
 */
public class GenericIndex extends AbstractIndex
{
    private GenericTable table;
    private boolean nonUnique;
    private String qualifier;
    private String indexName;
    private DBSIndexType indexType;
    private List<GenericIndexColumn> columns;

    public GenericIndex(
        GenericTable table,
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
    GenericIndex(GenericIndex source)
    {
        this.table = source.table;
        this.nonUnique = source.nonUnique;
        this.qualifier = source.qualifier;
        this.indexName = source.indexName;
        this.indexType = source.indexType;
        if (source.columns != null) {
            this.columns = new ArrayList<GenericIndexColumn>(source.columns.size());
            for (GenericIndexColumn sourceColumn : source.columns) {
                this.columns.add(new GenericIndexColumn(this, sourceColumn));
            }
        }
    }

    public GenericDataSource getDataSource()
    {
        return table.getDataSource();
    }

    @Property(name = "Table", viewable = true, order = 2)
    public GenericTable getTable()
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

    @Property(name = "Index Name", order = 1)
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

    public List<GenericIndexColumn> getColumns(DBRProgressMonitor monitor)
    {
        return columns;
    }

    public GenericIndexColumn getColumn(String columnName)
    {
        return DBUtils.findObject(columns, columnName);
    }

    void setColumns(List<GenericIndexColumn> columns)
    {
        this.columns = columns;
    }

    void addColumn(GenericIndexColumn column)
    {
        if (columns == null) {
            columns = new ArrayList<GenericIndexColumn>();
        }
        columns.add(column);
    }

    public boolean refreshEntity(DBRProgressMonitor monitor)
        throws DBException
    {
        return false;
    }

}
