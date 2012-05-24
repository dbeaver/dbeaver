/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPNamedObject;

/**
 * DBSConstraintModifyRule
 */
public class DBSConstraintModifyRule implements DBPNamedObject
{
    public static final DBSConstraintModifyRule UNKNOWN = new DBSConstraintModifyRule("UNKNOWN", "?", null); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBSConstraintModifyRule NO_ACTION = new DBSConstraintModifyRule("NO_ACTION", CoreMessages.model_struct_No_Action, null); //$NON-NLS-1$
    public static final DBSConstraintModifyRule CASCADE = new DBSConstraintModifyRule("CASCADE", CoreMessages.model_struct_Cascade, "CASCADE"); //$NON-NLS-1$ //$NON-NLS-3$
    public static final DBSConstraintModifyRule SET_NULL = new DBSConstraintModifyRule("SET_NULL", CoreMessages.model_struct_Set_NULL, "SET NULL"); //$NON-NLS-1$ //$NON-NLS-3$
    public static final DBSConstraintModifyRule SET_DEFAULT = new DBSConstraintModifyRule("SET_DEFAULT", CoreMessages.model_struct_Set_Default, "SET DEFAULT"); //$NON-NLS-1$ //$NON-NLS-3$
    public static final DBSConstraintModifyRule RESTRICT = new DBSConstraintModifyRule("RESTRICT", CoreMessages.model_struct_Restrict, "RESTRICT"); //$NON-NLS-1$ //$NON-NLS-3$

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

    @Override
    public String getName()
    {
        return name;
    }

    public String getClause()
    {
        return clause;
    }

    @Override
    public String toString()
    {
        return id;
    }
}
