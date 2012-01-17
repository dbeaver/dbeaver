/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.core.CoreMessages;

/**
 * DBSConstraintType
 */
public class DBSConstraintType
{
    public static final DBSConstraintType FOREIGN_KEY = new DBSConstraintType("fk", CoreMessages.model_struct_Foreign_Key, false); //$NON-NLS-1$
    public static final DBSConstraintType PRIMARY_KEY = new DBSConstraintType("pk", CoreMessages.model_struct_Primary_Key, true); //$NON-NLS-1$
    public static final DBSConstraintType UNIQUE_KEY = new DBSConstraintType("unique", CoreMessages.model_struct_Unique_Key, true); //$NON-NLS-1$
    public static final DBSConstraintType CHECK = new DBSConstraintType("check", CoreMessages.model_struct_Check, false); //$NON-NLS-1$
    public static final DBSConstraintType NOT_NULL = new DBSConstraintType("notnull", CoreMessages.model_struct_Not_NULL, false); //$NON-NLS-1$
    public static final DBSConstraintType INHERITANCE = new DBSConstraintType("inheritance", "Inheritance", false); //$NON-NLS-1$

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