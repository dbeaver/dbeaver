/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.core.CoreMessages;

/**
 * DBSEntityConstraintType
 */
public class DBSEntityConstraintType
{
    public static final DBSEntityConstraintType FOREIGN_KEY = new DBSEntityConstraintType("fk", "FOREIGN KEY", CoreMessages.model_struct_Foreign_Key, true, false); //$NON-NLS-1$
    public static final DBSEntityConstraintType PRIMARY_KEY = new DBSEntityConstraintType("pk", "PRIMARY KEY", CoreMessages.model_struct_Primary_Key, false, true); //$NON-NLS-1$
    public static final DBSEntityConstraintType UNIQUE_KEY = new DBSEntityConstraintType("unique", "UNIQUE KEY", CoreMessages.model_struct_Unique_Key, false, true); //$NON-NLS-1$
    public static final DBSEntityConstraintType VIRTUAL_KEY = new DBSEntityConstraintType("virtual", "VIRTUAL KEY", CoreMessages.model_struct_Virtual_Key, false, true); //$NON-NLS-1$
    public static final DBSEntityConstraintType PSEUDO_KEY = new DBSEntityConstraintType("pseudo", "PSEUDO", CoreMessages.model_struct_Pseudo_Key, false, true); //$NON-NLS-1$
    public static final DBSEntityConstraintType CHECK = new DBSEntityConstraintType("check", "CHECK", CoreMessages.model_struct_Check, false, false); //$NON-NLS-1$
    public static final DBSEntityConstraintType NOT_NULL = new DBSEntityConstraintType("notnull", "NOT NULL", CoreMessages.model_struct_Not_NULL, false, false); //$NON-NLS-1$
    public static final DBSEntityConstraintType INDEX = new DBSEntityConstraintType("index", "Index", "Index", false, false); //$NON-NLS-1$
    public static final DBSEntityConstraintType ASSOCIATION = new DBSEntityConstraintType("association", "Association", "Association", true, false); //$NON-NLS-1$
    public static final DBSEntityConstraintType INHERITANCE = new DBSEntityConstraintType("inheritance", "Inheritance", "Inheritance", true, false); //$NON-NLS-1$

    private final String id;
    private final String name;
    private final String localizedName;
    private final boolean association;
    private final boolean unique;

    public DBSEntityConstraintType(String id, String name, String localizedName, boolean association, boolean unique)
    {
        this.id = id;
        this.name = name;
        this.localizedName = localizedName == null ? name : localizedName;
        this.association = association;
        this.unique = unique;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLocalizedName()
    {
        return localizedName;
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