/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.core.CoreMessages;

/**
 * DBSEntityConstraintType
 */
public class DBSEntityConstraintType
{
    public static final DBSEntityConstraintType FOREIGN_KEY = new DBSEntityConstraintType("fk", CoreMessages.model_struct_Foreign_Key, true, false); //$NON-NLS-1$
    public static final DBSEntityConstraintType PRIMARY_KEY = new DBSEntityConstraintType("pk", CoreMessages.model_struct_Primary_Key, false, true); //$NON-NLS-1$
    public static final DBSEntityConstraintType UNIQUE_KEY = new DBSEntityConstraintType("unique", CoreMessages.model_struct_Unique_Key, false, true); //$NON-NLS-1$
    public static final DBSEntityConstraintType CHECK = new DBSEntityConstraintType("check", CoreMessages.model_struct_Check, false, false); //$NON-NLS-1$
    public static final DBSEntityConstraintType NOT_NULL = new DBSEntityConstraintType("notnull", CoreMessages.model_struct_Not_NULL, false, false); //$NON-NLS-1$
    public static final DBSEntityConstraintType ASSOCIATION = new DBSEntityConstraintType("association", "Association", true, false); //$NON-NLS-1$
    public static final DBSEntityConstraintType INHERITANCE = new DBSEntityConstraintType("inheritance", "Inheritance", true, false); //$NON-NLS-1$

    private final String id;
    private final String name;
    private final boolean association;
    private final boolean unique;

    public DBSEntityConstraintType(String id, String name, boolean association, boolean unique)
    {
        this.id = id;
        this.name = name;
        this.association = association;
        this.unique = unique;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isAssociation()
    {
        return association;
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