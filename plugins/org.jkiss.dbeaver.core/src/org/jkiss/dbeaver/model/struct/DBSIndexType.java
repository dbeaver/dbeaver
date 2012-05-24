/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPNamedObject;

/**
 * DBSIndexType
 */
public class DBSIndexType implements DBPNamedObject
{
    public static final DBSIndexType UNKNOWN = new DBSIndexType("UNKNOWN", CoreMessages.model_struct_Unknown); //$NON-NLS-1$
    public static final DBSIndexType STATISTIC = new DBSIndexType("STATISTIC", CoreMessages.model_struct_Statistic); //$NON-NLS-1$
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
