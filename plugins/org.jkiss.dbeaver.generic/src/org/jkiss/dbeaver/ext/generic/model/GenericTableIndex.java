/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableIndex;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSIndexType;

import java.util.ArrayList;
import java.util.List;

/**
 * GenericTable
 */
public class GenericTableIndex extends JDBCTableIndex<GenericTable>
{
    private boolean nonUnique;
    private String qualifier;
    private long cardinality;
    private List<GenericTableIndexColumn> columns;

    public GenericTableIndex(
        GenericTable table,
        boolean nonUnique,
        String qualifier,
        long cardinality,
        String indexName,
        DBSIndexType indexType,
        boolean persisted)
    {
        super(table, indexName, indexType, persisted);
        this.nonUnique = nonUnique;
        this.qualifier = qualifier;
        this.cardinality = cardinality;
    }

    /**
     * Copy constructor
     * @param source source index
     */
    GenericTableIndex(GenericTableIndex source)
    {
        super(source);
        this.nonUnique = source.nonUnique;
        this.qualifier = source.qualifier;
        this.cardinality = source.cardinality;
        if (source.columns != null) {
            this.columns = new ArrayList<GenericTableIndexColumn>(source.columns.size());
            for (GenericTableIndexColumn sourceColumn : source.columns) {
                this.columns.add(new GenericTableIndexColumn(this, sourceColumn));
            }
        }
    }

    @Override
    public GenericDataSource getDataSource()
    {
        return getTable().getDataSource();
    }

    @Override
    @Property(name = "Index Description", viewable = true, order = 100)
    public String getDescription()
    {
        return null;
    }

    @Override
    @Property(name = "Unique", viewable = true, order = 4)
    public boolean isUnique()
    {
        return !nonUnique;
    }

    @Property(name = "Qualifier", viewable = true, order = 5)
    public String getQualifier()
    {
        return qualifier;
    }

    @Property(name = "Cardinality", viewable = true, order = 5)
    public long getCardinality()
    {
        return cardinality;
    }

    @Override
    public List<GenericTableIndexColumn> getColumns(DBRProgressMonitor monitor)
    {
        return columns;
    }

    public GenericTableIndexColumn getColumn(String columnName)
    {
        return DBUtils.findObject(columns, columnName);
    }

    void setColumns(List<GenericTableIndexColumn> columns)
    {
        this.columns = columns;
    }

    public void addColumn(GenericTableIndexColumn column)
    {
        if (columns == null) {
            columns = new ArrayList<GenericTableIndexColumn>();
        }
        columns.add(column);
    }

    @Override
    public String getFullQualifiedName()
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getTable().getCatalog(),
            getTable().getSchema(),
            this);
    }

}
