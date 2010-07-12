/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

/**
 * DBSManipulationType
 */
public class DBSManipulationType
{
    public static final DBSManipulationType INSERT = new DBSManipulationType("INSERT");
    public static final DBSManipulationType DELETE = new DBSManipulationType("DELETE");
    public static final DBSManipulationType UPDATE = new DBSManipulationType("UPDATE");
    public static final DBSManipulationType UNKNOWN = new DBSManipulationType("UNKNOWN");

    private final String name;

    protected DBSManipulationType(String name)
    {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String toString()
    {
        return getName();
    }

    public static DBSManipulationType getByName(String name)
    {
        if (name.toUpperCase().equals(INSERT.getName())) {
            return INSERT;
        } else if (name.toUpperCase().equals(DELETE.getName())) {
            return DELETE;
        } if (name.toUpperCase().equals(UPDATE.getName())) {
            return UPDATE;
        } else {
            return UNKNOWN;
        }
    }
}