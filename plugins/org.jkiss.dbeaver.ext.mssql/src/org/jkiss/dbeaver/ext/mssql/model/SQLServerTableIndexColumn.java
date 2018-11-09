/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.mssql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.struct.AbstractTableIndexColumn;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndexColumn;

/**
 * SQLServerTableIndexColumn
 */
public class SQLServerTableIndexColumn extends AbstractTableIndexColumn
{
    private SQLServerTableIndex index;
    private SQLServerTableColumn tableColumn;
    private int ordinalPosition;
    private boolean ascending;
    private boolean nullable;
    private String subPart;

    public SQLServerTableIndexColumn(
        SQLServerTableIndex index,
        SQLServerTableColumn tableColumn,
        int ordinalPosition,
        boolean ascending,
        boolean nullable,
        String subPart)
    {
        this.index = index;
        this.tableColumn = tableColumn;
        this.ordinalPosition = ordinalPosition;
        this.ascending = ascending;
        this.nullable = nullable;
        this.subPart = subPart;
    }

    SQLServerTableIndexColumn(DBRProgressMonitor monitor, SQLServerTableIndex toIndex, DBSTableIndexColumn source) throws DBException {
        this.index = toIndex;
        if (source.getTableColumn() != null) {
            this.tableColumn = toIndex.getTable().getAttribute(monitor, source.getTableColumn().getName());
        }
        this.ordinalPosition = source.getOrdinalPosition();
        this.ascending = source.isAscending();
        if (source instanceof SQLServerTableIndexColumn) {
            this.nullable = ((SQLServerTableIndexColumn)source).nullable;
            this.subPart = ((SQLServerTableIndexColumn)source).subPart;
        }
    }

    @NotNull
    @Override
    public SQLServerTableIndex getIndex()
    {
        return index;
    }

    //@Property(name = "Name", viewable = true, order = 1)
    @NotNull
    @Override
    public String getName()
    {
        return tableColumn.getName();
    }

    @Nullable
    @Override
    @Property(id = "name", viewable = true, order = 1)
    public SQLServerTableColumn getTableColumn()
    {
        return tableColumn;
    }

    @Override
    @Property(viewable = false, order = 2)
    public int getOrdinalPosition()
    {
        return ordinalPosition;
    }

    @Override
    @Property(viewable = true, order = 3)
    public boolean isAscending()
    {
        return ascending;
    }

    @Property(viewable = true, order = 4)
    public boolean isNullable()
    {
        return nullable;
    }

    @Property(viewable = true, order = 5)
    public String getSubPart() {
        return subPart;
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return tableColumn.getDescription();
    }

    @Override
    public SQLServerTableIndex getParentObject()
    {
        return index;
    }

    @NotNull
    @Override
    public SQLServerDataSource getDataSource()
    {
        return index.getDataSource();
    }

}
