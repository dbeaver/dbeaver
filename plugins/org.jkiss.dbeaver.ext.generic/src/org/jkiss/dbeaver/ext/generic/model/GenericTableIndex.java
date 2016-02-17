/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.generic.model;

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
 * GenericTable
 */
public class GenericTableIndex extends JDBCTableIndex<GenericStructContainer, GenericTable>
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
        super(table.getContainer(), table, indexName, indexType, persisted);
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
            this.columns = new ArrayList<>(source.columns.size());
            for (GenericTableIndexColumn sourceColumn : source.columns) {
                this.columns.add(new GenericTableIndexColumn(this, sourceColumn));
            }
        }
    }

    @NotNull
    @Override
    public GenericDataSource getDataSource()
    {
        return getTable().getDataSource();
    }

    @Nullable
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
    public List<GenericTableIndexColumn> getAttributeReferences(DBRProgressMonitor monitor)
    {
        return columns;
    }

    @Nullable
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
            columns = new ArrayList<>();
        }
        columns.add(column);
    }

    @NotNull
    @Override
    public String getFullQualifiedName()
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getTable().getCatalog(),
            getTable().getSchema(),
            this);
    }

}
