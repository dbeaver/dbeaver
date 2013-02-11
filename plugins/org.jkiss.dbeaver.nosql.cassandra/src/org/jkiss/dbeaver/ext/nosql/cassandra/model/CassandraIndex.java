/*
 * Copyright (C) 2010-2012 Serge Rieder
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
package org.jkiss.dbeaver.ext.nosql.cassandra.model;

import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableIndex;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * CassandraIndex
 */
public class CassandraIndex extends JDBCTableIndex<CassandraColumnFamily>
{
    private boolean nonUnique;
    private String qualifier;
    private long cardinality;
    private List<CassandraIndexColumn> columns;

    public CassandraIndex(
        CassandraColumnFamily table,
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
    CassandraIndex(CassandraIndex source)
    {
        super(source);
        this.nonUnique = source.nonUnique;
        this.qualifier = source.qualifier;
        this.cardinality = source.cardinality;
        if (source.columns != null) {
            this.columns = new ArrayList<CassandraIndexColumn>(source.columns.size());
            for (CassandraIndexColumn sourceColumn : source.columns) {
                this.columns.add(new CassandraIndexColumn(this, sourceColumn));
            }
        }
    }

    @Override
    public CassandraDataSource getDataSource()
    {
        return getTable().getDataSource();
    }

    @Override
    @Property(viewable = true, order = 100)
    public String getDescription()
    {
        return null;
    }

    @Override
    @Property(viewable = true, order = 4)
    public boolean isUnique()
    {
        return !nonUnique;
    }

    @Property(viewable = true, order = 5)
    public String getQualifier()
    {
        return qualifier;
    }

    @Property(viewable = true, order = 5)
    public long getCardinality()
    {
        return cardinality;
    }

    @Override
    public Collection<CassandraIndexColumn> getAttributeReferences(DBRProgressMonitor monitor)
    {
        return columns;
    }

    public CassandraIndexColumn getColumn(String columnName)
    {
        return DBUtils.findObject(columns, columnName);
    }

    void setColumns(List<CassandraIndexColumn> columns)
    {
        this.columns = columns;
    }

    public void addColumn(CassandraIndexColumn column)
    {
        if (columns == null) {
            columns = new ArrayList<CassandraIndexColumn>();
        }
        columns.add(column);
    }

    @Override
    public String getFullQualifiedName()
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getTable().getSchema(),
            this);
    }

}
