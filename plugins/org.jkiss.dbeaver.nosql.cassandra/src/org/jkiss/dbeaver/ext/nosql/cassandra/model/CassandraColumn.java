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

import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCColumnKeyType;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableColumn;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableColumn;

/**
 * CassandraColumnFamily
 */
public class CassandraColumn extends JDBCTableColumn<CassandraColumnFamily> implements DBSTableColumn, JDBCColumnKeyType
{
    private int radix;
    private String remarks;
    private int sourceType;
    private long charLength;

    public CassandraColumn(CassandraColumnFamily table)
    {
        super(table, false);
    }

    public CassandraColumn(
        CassandraColumnFamily table,
        String columnName,
        String typeName,
        int valueType,
        int sourceType,
        int ordinalPosition,
        long columnSize,
        long charLength,
        int scale,
        int precision,
        int radix,
        boolean notNull,
        String remarks)
    {
        super(table,
            true,
            columnName,
            typeName,
            valueType,
            ordinalPosition,
            columnSize,
            scale,
            precision,
            notNull,
            null);
        this.sourceType = sourceType;
        this.charLength = charLength;
        this.remarks = remarks;
        this.radix = radix;
    }

    @Override
    public CassandraDataSource getDataSource()
    {
        return getTable().getDataSource();
    }

    public int getSourceType()
    {
        return sourceType;
    }

    public long getCharLength()
    {
        return charLength;
    }

    @Override
    @Property(viewable = true, order = 51)
    public boolean isSequence()
    {
        return false;
    }

    @Override
    public JDBCColumnKeyType getKeyType()
    {
        return this;
    }

    @Property(viewable = false, order = 62)
    public int getRadix()
    {
        return radix;
    }

    public void setRadix(int radix)
    {
        this.radix = radix;
    }

    @Override
    @Property(viewable = true, order = 80)
    public boolean isInUniqueKey()
    {
        return false;
    }

    @Override
    public boolean isInReferenceKey()
    {
        return false;
    }

    @Override
    @Property(viewable = true, order = 100)
    public String getDescription()
    {
        return remarks;
    }

    @Override
    public String toString()
    {
        return getTable().getFullQualifiedName() + "." + getName();
    }

}
