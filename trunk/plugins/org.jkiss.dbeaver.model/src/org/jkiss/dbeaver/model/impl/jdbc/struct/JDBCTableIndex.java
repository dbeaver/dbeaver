/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.model.impl.jdbc.struct;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.struct.AbstractTableIndex;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;

/**
 * JDBC abstract index
 */
public abstract class JDBCTableIndex<CONTAINER extends DBSObjectContainer, TABLE extends JDBCTable>
    extends AbstractTableIndex
    implements DBPSaveableObject
{
    private final CONTAINER container;
    private final TABLE table;
    protected String name;
    protected DBSIndexType indexType;
    private boolean persisted;

    protected JDBCTableIndex(CONTAINER container, TABLE table, String name, @Nullable DBSIndexType indexType, boolean persisted) {
        this.container = container;
        this.table = table;
        this.name = name;
        this.indexType = indexType;
        this.persisted = persisted;
    }

    protected JDBCTableIndex(JDBCTableIndex<CONTAINER, TABLE> source)
    {
        this.container = source.container;
        this.table = source.table;
        this.name = source.name;
        this.indexType = source.indexType;
        this.persisted = source.persisted;
    }

    @Override
    public CONTAINER getContainer()
    {
        return container;
    }

    @Override
    public TABLE getParentObject()
    {
        return table;
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
    public String getName()
    {
        return name;
    }

    public void setName(String indexName)
    {
        this.name = indexName;
    }

    @Override
    @Property(viewable = true, order = 2)
    public TABLE getTable()
    {
        return table;
    }

    @Override
    @Property(viewable = true, order = 3)
    public DBSIndexType getIndexType()
    {
        return this.indexType;
    }

    @Override
    public boolean isPersisted()
    {
        return persisted;
    }

    @Override
    public void setPersisted(boolean persisted)
    {
        this.persisted = persisted;
    }

}
