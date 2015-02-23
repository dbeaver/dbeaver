/*
 * Copyright (C) 2010-2015 Serge Rieder
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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableIndex;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * CassandraIndex
 */
public class CassandraIndex extends JDBCTableIndex<CassandraKeyspace, CassandraColumnFamily>
{
    private CassandraIndexColumn column;

    public CassandraIndex(
        CassandraColumn column)
    {
        super(column.getTable().getContainer(), column.getTable(), column.getIndexName(), new DBSIndexType(column.getIndexType(), column.getIndexType()), true);
        this.column = new CassandraIndexColumn(this, column);
    }

    @NotNull
    @Override
    public CassandraDataSource getDataSource()
    {
        return getTable().getDataSource();
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return null;
    }

    @Override
    @Property(viewable = true, order = 4)
    public boolean isUnique()
    {
        return false;
    }

    @Property(viewable = false, order = 5)
    public Object getIndexOptions()
    {
        return column.getTableColumn().getIndexOptions();
    }

    @Override
    public List<CassandraIndexColumn> getAttributeReferences(DBRProgressMonitor monitor)
    {
        return Collections.singletonList(column);
    }

    @Override
    public String getFullQualifiedName()
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getTable().getSchema(),
            this);
    }

}
