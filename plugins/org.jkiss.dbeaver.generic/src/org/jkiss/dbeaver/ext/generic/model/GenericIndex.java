/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.model;

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
public class GenericIndex extends JDBCIndex<GenericTable>
{
    private boolean nonUnique;
    private String qualifier;
    private long cardinality;
    private List<GenericIndexColumn> columns;

    public GenericIndex(
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
    GenericIndex(GenericIndex source)
    {
        super(source);
        this.nonUnique = source.nonUnique;
        this.qualifier = source.qualifier;
        this.cardinality = source.cardinality;
        if (source.columns != null) {
            this.columns = new ArrayList<GenericIndexColumn>(source.columns.size());
            for (GenericIndexColumn sourceColumn : source.columns) {
                this.columns.add(new GenericIndexColumn(this, sourceColumn));
            }
        }
    }

    public GenericDataSource getDataSource()
    {
        return getTable().getDataSource();
    }

    @Property(name = "Index Description", viewable = true, order = 100)
    public String getDescription()
    {
        return null;
    }

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

    public void addColumn(GenericIndexColumn column)
    {
        if (columns == null) {
            columns = new ArrayList<GenericIndexColumn>();
        }
        columns.add(column);
    }

    public String getFullQualifiedName()
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getTable().getCatalog(),
            getTable().getSchema(),
            this);
    }

}
