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
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.impl.struct.AbstractTableIndexColumn;
import org.jkiss.dbeaver.model.meta.Property;

/**
 * GenericIndexColumn
 */
public class OracleTableIndexColumn extends AbstractTableIndexColumn
{
    private OracleTableIndex index;
    private OracleTableColumn tableColumn;
    private int ordinalPosition;
    private boolean ascending;

    public OracleTableIndexColumn(
        OracleTableIndex index,
        OracleTableColumn tableColumn,
        int ordinalPosition,
        boolean ascending)
    {
        this.index = index;
        this.tableColumn = tableColumn;
        this.ordinalPosition = ordinalPosition;
        this.ascending = ascending;
    }

    OracleTableIndexColumn(OracleTableIndex toIndex, OracleTableIndexColumn source)
    {
        this.index = toIndex;
        this.tableColumn = source.tableColumn;
        this.ordinalPosition = source.ordinalPosition;
        this.ascending = source.ascending;
    }

    @NotNull
    @Override
    public OracleTableIndex getIndex()
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
    public OracleTableColumn getTableColumn()
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

    @Nullable
    @Override
    public String getDescription()
    {
        return tableColumn.getDescription();
    }

    @Override
    public OracleTableIndex getParentObject()
    {
        return index;
    }

    @NotNull
    @Override
    public OracleDataSource getDataSource()
    {
        return index.getDataSource();
    }

}
