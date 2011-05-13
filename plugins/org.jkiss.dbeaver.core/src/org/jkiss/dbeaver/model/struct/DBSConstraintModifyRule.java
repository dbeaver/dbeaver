/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.model.DBPNamedObject;

/**
 * DBSConstraintModifyRule
 */
public class DBSConstraintModifyRule implements DBPNamedObject
{
    public static final DBSConstraintModifyRule UNKNOWN = new DBSConstraintModifyRule("UNKNOWN", "?");
    public static final DBSConstraintModifyRule NO_ACTION = new DBSConstraintModifyRule("NO_ACTION", "No Action");
    public static final DBSConstraintModifyRule CASCADE = new DBSConstraintModifyRule("CASCADE", "Cascade");
    public static final DBSConstraintModifyRule SET_NULL = new DBSConstraintModifyRule("SET_NULL", "Set NULL");
    public static final DBSConstraintModifyRule SET_DEFAULT = new DBSConstraintModifyRule("SET_DEFAULT", "Set Default");
    public static final DBSConstraintModifyRule RESTRICT = new DBSConstraintModifyRule("RESTRICT", "Restrict");

    private final String id;
    private final String name;

    public DBSConstraintModifyRule(String id, String name)
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
}
