/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

/**
 * DBSIndexType
 */
public class DBSIndexType
{
    public static final DBSIndexType UNKNOWN = new DBSIndexType("UNKNOWN", "Unknown");
    public static final DBSIndexType STATISTIC = new DBSIndexType("STATISTIC", "Statistic");
    public static final DBSIndexType CLUSTERED = new DBSIndexType("CLUSTERED", "Clustered");
    public static final DBSIndexType HASHED = new DBSIndexType("HASHED", "Hashed");
    public static final DBSIndexType OTHER = new DBSIndexType("OTHER", "Other");

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
