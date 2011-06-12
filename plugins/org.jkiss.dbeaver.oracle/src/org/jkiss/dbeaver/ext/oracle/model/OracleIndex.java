/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCIndex;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSIndexType;

import java.util.ArrayList;
import java.util.List;

/**
 * OracleIndex
 */
public class OracleIndex extends JDBCIndex<OracleTable>
{
    private boolean nonUnique;
    private List<OracleIndexColumn> columns;

    public OracleIndex(
        OracleTable table,
        DBSIndexType indexType)
    {
        super(table, null, indexType, false);
    }

    public OracleIndex(
        OracleTable table,
        boolean nonUnique,
        String indexName,
        DBSIndexType indexType)
    {
        super(table, indexName, indexType, true);
        this.nonUnique = nonUnique;
    }

    /**
     * Copy constructor
     * @param source source index
     */
    OracleIndex(OracleIndex source)
    {
        super(source);
        this.nonUnique = source.nonUnique;
        if (source.columns != null) {
            this.columns = new ArrayList<OracleIndexColumn>(source.columns.size());
            for (OracleIndexColumn sourceColumn : source.columns) {
                this.columns.add(new OracleIndexColumn(this, sourceColumn));
            }
        }
    }

    public OracleDataSource getDataSource()
    {
        return getTable().getDataSource();
    }

    @Property(name = "Unique", viewable = true, order = 5)
    public boolean isUnique()
    {
        return !nonUnique;
    }

    public String getDescription()
    {
        return "";
    }

    public List<OracleIndexColumn> getColumns(DBRProgressMonitor monitor)
    {
        return columns;
    }

    public OracleIndexColumn getColumn(String columnName)
    {
        return DBUtils.findObject(columns, columnName);
    }

    void setColumns(List<OracleIndexColumn> columns)
    {
        this.columns = columns;
    }

    public void addColumn(OracleIndexColumn column)
    {
        if (columns == null) {
            columns = new ArrayList<OracleIndexColumn>();
        }
        columns.add(column);
    }

    public String getFullQualifiedName()
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getTable().getContainer(),
            this);
    }

    @Override
    public String toString()
    {
        return getFullQualifiedName();
    }
}
