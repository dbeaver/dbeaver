/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.model.struct.rdb;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.DBPNamedObject;

/**
 * DBSIndexType
 */
public class DBSIndexType implements DBPNamedObject
{
    public static final DBSIndexType UNKNOWN = new DBSIndexType("UNKNOWN", ModelMessages.model_struct_Unknown); //$NON-NLS-1$
    //public static final DBSIndexType STATISTIC = new DBSIndexType("STATISTIC", CoreMessages.model_struct_Statistic); //$NON-NLS-1$
    public static final DBSIndexType CLUSTERED = new DBSIndexType("CLUSTERED", ModelMessages.model_struct_Clustered); //$NON-NLS-1$
    public static final DBSIndexType HASHED = new DBSIndexType("HASHED", ModelMessages.model_struct_Hashed); //$NON-NLS-1$
    public static final DBSIndexType OTHER = new DBSIndexType("OTHER", ModelMessages.model_struct_Other); //$NON-NLS-1$

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

    @NotNull
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
