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
public class CassandraColumn extends JDBCTableColumn<CassandraColumnFamily> implements DBSTableColumn
{
    public static enum KeyType implements JDBCColumnKeyType {
        PRIMARY,
        SECONDARY;

        @Override
        public boolean isInUniqueKey()
        {
            return this == PRIMARY;
        }

        @Override
        public boolean isInReferenceKey()
        {
            return false;
        }
    }

    private String remarks;
    private KeyType keyType;

    public CassandraColumn(
        CassandraColumnFamily table,
        KeyType keyType,
        String columnName,
        String typeName,
        int valueType,
        int ordinalPosition,
        long columnSize,
        int scale,
        int precision,
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
        this.keyType = keyType;
        this.remarks = remarks;
    }

    @Override
    public CassandraDataSource getDataSource()
    {
        return getTable().getDataSource();
    }

    @Override
    public boolean isSequence()
    {
        return false;
    }

    @Override
    public KeyType getKeyType()
    {
        return keyType;
    }

    @Override
    @Property(viewable = true, order = 100)
    public String getDescription()
    {
        return remarks;
    }

    // Override to hide property
    @Override
    public String getDefaultValue()
    {
        return super.getDefaultValue();
    }

    @Override
    public String toString()
    {
        return getTable().getFullQualifiedName() + "." + getName();
    }

}
