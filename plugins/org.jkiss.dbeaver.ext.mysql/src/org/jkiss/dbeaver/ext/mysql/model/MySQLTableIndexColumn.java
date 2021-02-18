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
package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.struct.AbstractTableIndexColumn;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndexColumn;

/**
 * GenericIndexColumn
 */
public class MySQLTableIndexColumn extends AbstractTableIndexColumn
{
    private MySQLTableIndex index;
    private MySQLTableColumn tableColumn;
    private int ordinalPosition;
    private boolean ascending;
    private boolean nullable;
    private String subPart;

    public MySQLTableIndexColumn(
        MySQLTableIndex index,
        MySQLTableColumn tableColumn,
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

    MySQLTableIndexColumn(DBRProgressMonitor monitor, MySQLTableIndex toIndex, DBSTableIndexColumn source) throws DBException {
        this.index = toIndex;
        if (source.getTableColumn() != null) {
            this.tableColumn = toIndex.getTable().getAttribute(monitor, source.getTableColumn().getName());
        }
        this.ordinalPosition = source.getOrdinalPosition();
        this.ascending = source.isAscending();
        if (source instanceof MySQLTableIndexColumn) {
            this.nullable = ((MySQLTableIndexColumn)source).nullable;
            this.subPart = ((MySQLTableIndexColumn)source).subPart;
        }
    }

    @NotNull
    @Override
    public MySQLTableIndex getIndex()
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
    public MySQLTableColumn getTableColumn()
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
    public MySQLTableIndex getParentObject()
    {
        return index;
    }

    @NotNull
    @Override
    public MySQLDataSource getDataSource()
    {
        return index.getDataSource();
    }

}
