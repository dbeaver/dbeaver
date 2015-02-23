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
package org.jkiss.dbeaver.model.struct.rdb;

import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPNamedObject;

/**
 * DBSIndexType
 */
public class DBSIndexType implements DBPNamedObject
{
    public static final DBSIndexType UNKNOWN = new DBSIndexType("UNKNOWN", CoreMessages.model_struct_Unknown); //$NON-NLS-1$
    //public static final DBSIndexType STATISTIC = new DBSIndexType("STATISTIC", CoreMessages.model_struct_Statistic); //$NON-NLS-1$
    public static final DBSIndexType CLUSTERED = new DBSIndexType("CLUSTERED", CoreMessages.model_struct_Clustered); //$NON-NLS-1$
    public static final DBSIndexType HASHED = new DBSIndexType("HASHED", CoreMessages.model_struct_Hashed); //$NON-NLS-1$
    public static final DBSIndexType OTHER = new DBSIndexType("OTHER", CoreMessages.model_struct_Other); //$NON-NLS-1$

    private final String id;
    private final String name;

    public DBSIndexType(String id, String name)
    {
        this.id = id;
        this.name = name;
    }

    public String getId()
    {
        return id;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public String toString()
    {
        return name;
    }

    @Override
    public int hashCode()
    {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        return obj instanceof DBSIndexType && name.equals(((DBSIndexType)obj).name);
    }
}
