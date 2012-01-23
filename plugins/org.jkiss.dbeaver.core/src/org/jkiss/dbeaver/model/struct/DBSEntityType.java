/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

/**
 * Entity type
 */
public class DBSEntityType
{
    public static final DBSEntityType TABLE = new DBSEntityType("table", "Table", true); //$NON-NLS-1$
    public static final DBSEntityType TYPE = new DBSEntityType("type", "Type", true); //$NON-NLS-1$
    public static final DBSEntityType CLASS = new DBSEntityType("class", "Class", false); //$NON-NLS-1$
    public static final DBSEntityType ASSOCIATION = new DBSEntityType("association", "Association", false); //$NON-NLS-1$

    private final String id;
    private final String name;
    private final boolean physical;

    public DBSEntityType(String id, String name, boolean physical)
    {
        this.id = id;
        this.name = name;
        this.physical = physical;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isPhysical()
    {
        return physical;
    }

    public String toString()
    {
        return getName();
    }
}