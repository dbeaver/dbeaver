/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.struct.AbstractTableIndexColumn;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * GenericIndexColumn
 */
public class PostgreIndexColumn extends AbstractTableIndexColumn
{
    private PostgreIndex index;
    private PostgreAttribute tableColumn;
    private String expression;
    private int ordinalPosition;
    private boolean ascending;
    private long opClass;
    private boolean nullable;

    public PostgreIndexColumn(
            PostgreIndex index,
            PostgreAttribute tableColumn,
            String expression,
            int ordinalPosition,
            boolean ascending,
            long opClass,
            boolean nullable)
    {
        this.index = index;
        this.tableColumn = tableColumn;
        this.expression = expression;
        this.ordinalPosition = ordinalPosition;
        this.ascending = ascending;
        this.opClass = opClass;
        this.nullable = nullable;
    }

    @NotNull
    @Override
    public PostgreIndex getIndex()
    {
        return index;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName()
    {
        return tableColumn == null ? expression : tableColumn.getName();
    }

    @Nullable
    @Override
    @Property(viewable = true, order = 1)
    public PostgreAttribute getTableColumn()
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

    @Property(viewable = true, order = 6)
    public PostgreOperatorClass getOperatorClass(DBRProgressMonitor monitor) throws DBException {
        if (opClass <= 0) {
            return null;
        }
        PostgreAccessMethod accessMethod = index.getAccessMethod(monitor);
        if (accessMethod == null) {
            return null;
        }
        return accessMethod.getOperatorClass(monitor, opClass);
    }



    @Nullable
    @Override
    public String getDescription()
    {
        return tableColumn == null ? null : tableColumn.getDescription();
    }

    @Override
    public PostgreIndex getParentObject()
    {
        return index;
    }

    @NotNull
    @Override
    public PostgreDataSource getDataSource()
    {
        return index.getDataSource();
    }

    @Override
    public String toString() {
        return tableColumn == null ? "NULL" : tableColumn.toString();
    }
}
