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
package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.dbeaver.core.Log;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * MySQLParameter
 */
public class MySQLParameter implements DBSObject
{
    static final Log log = Log.getLog(MySQLParameter.class);

    private final MySQLDataSource dataSource;
    private final String name;
    private Object value;
    private String description;

    public MySQLParameter(MySQLDataSource dataSource, String name, Object value)
    {
        this.dataSource = dataSource;
        this.name = name;
        this.value = value;
    }

    @Override
    @Property(viewable = true, order = 1)
    public String getName()
    {
        return name;
    }

    @Property(viewable = true, order = 2)
    public Object getValue()
    {
        return value;
    }

//    @Property(name = "Description", viewable = true, order = 100)
    @Nullable
    @Override
    public String getDescription()
    {
        return description;
    }

    @Override
    public DBSObject getParentObject()
    {
        return getDataSource();
    }

    @NotNull
    @Override
    public MySQLDataSource getDataSource()
    {
        return dataSource;
    }

    @Override
    public boolean isPersisted()
    {
        return true;
    }
}
