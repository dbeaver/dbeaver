/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableIndex;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;

import java.util.ArrayList;
import java.util.List;

/**
 * GenericTable
 */
public class GenericTableIndex extends JDBCTableIndex<GenericStructContainer, GenericTableBase>
{
    private boolean nonUnique;
    private String qualifier;
    private long cardinality;
    private List<GenericTableIndexColumn> columns;

    public GenericTableIndex(
        GenericTableBase table,
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
    @Property(viewable = true, length = PropertyLength.MULTILINE, order = 100)
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

    public void setUnique(boolean unique) {
        this.nonUnique = !unique;
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

    public void setColumns(List<GenericTableIndexColumn> columns)
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
    public String getFullyQualifiedName(DBPEvaluationContext context)
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getTable().getCatalog(),
            getTable().getSchema(),
            this);
    }

}
