/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

/**
 * DBSConstraintType
 */
public class DBSConstraintType
{
    public static final DBSConstraintType FOREIGN_KEY = new DBSConstraintType("Foreign Key", false);
    public static final DBSConstraintType PRIMARY_KEY = new DBSConstraintType("Primary Key", true);
    public static final DBSConstraintType UNIQUE_KEY = new DBSConstraintType("Unique Key", true);
    public static final DBSConstraintType CHECK = new DBSConstraintType("Check", false);
    public static final DBSConstraintType NOT_NULL = new DBSConstraintType("Not NULL", false);

    private final String name;
    private final boolean unique;

    protected DBSConstraintType(String name, boolean unique)
    {
        this.name = name;
        this.unique = unique;
    }

    public String getName() {
        return name;
    }

    public boolean isUnique()
    {
        return unique;
    }

    public String toString()
    {
        return getName();
    }
}