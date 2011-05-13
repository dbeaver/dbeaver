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
    public static final DBSConstraintModifyRule UNKNOWN = new DBSConstraintModifyRule("UNKNOWN", "?", null);
    public static final DBSConstraintModifyRule NO_ACTION = new DBSConstraintModifyRule("NO_ACTION", "No Action", null);
    public static final DBSConstraintModifyRule CASCADE = new DBSConstraintModifyRule("CASCADE", "Cascade", "CASCADE");
    public static final DBSConstraintModifyRule SET_NULL = new DBSConstraintModifyRule("SET_NULL", "Set NULL", "SET NULL");
    public static final DBSConstraintModifyRule SET_DEFAULT = new DBSConstraintModifyRule("SET_DEFAULT", "Set Default", "SET DEFAULT");
    public static final DBSConstraintModifyRule RESTRICT = new DBSConstraintModifyRule("RESTRICT", "Restrict", "RESTRICT");

    private final String id;
    private final String name;
    private final String clause;

    public DBSConstraintModifyRule(String id, String name, String clause)
    {
        this.id = id;
        this.name = name;
        this.clause = clause;
    }

    public String getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public String getClause()
    {
        return clause;
    }
}
