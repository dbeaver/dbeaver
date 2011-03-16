/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

/**
 * DBSConstraintType
 */
public class DBSConstraintType
{
    public static final DBSConstraintType FOREIGN_KEY = new DBSConstraintType("fk", "Foreign Key", false);
    public static final DBSConstraintType PRIMARY_KEY = new DBSConstraintType("pk", "Primary Key", true);
    public static final DBSConstraintType UNIQUE_KEY = new DBSConstraintType("unique", "Unique Key", true);
    public static final DBSConstraintType CHECK = new DBSConstraintType("check", "Check", false);
    public static final DBSConstraintType NOT_NULL = new DBSConstraintType("notnull", "Not NULL", false);

    private final String id;
    private final String name;
    private final boolean unique;

    public DBSConstraintType(String id, String name, boolean unique)
    {
        this.id = id;
        this.name = name;
        this.unique = unique;
    }

    public String getId() {
        return id;
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