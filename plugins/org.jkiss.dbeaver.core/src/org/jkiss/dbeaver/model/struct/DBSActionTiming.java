/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

/**
 * DBSConstraintType
 */
public class DBSActionTiming
{
    public static final DBSActionTiming BEFORE = new DBSActionTiming("BEFORE");
    public static final DBSActionTiming AFTER = new DBSActionTiming("AFTER");
    public static final DBSActionTiming UNKNOWN = new DBSActionTiming("UNKNOWN");

    private final String name;

    protected DBSActionTiming(String name)
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

    public static DBSActionTiming getByName(String name)
    {
        if (name.toUpperCase().equals(BEFORE.getName())) {
            return BEFORE;
        } else if (name.toUpperCase().equals(AFTER.getName())) {
            return AFTER;
        } else {
            return UNKNOWN;
        }
    }
}