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
package org.jkiss.dbeaver.model.struct.rdb;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.DBPNamedObject;

/**
 * DBSForeignKeyModifyRule
 */
public class DBSForeignKeyModifyRule implements DBPNamedObject
{
    public static final DBSForeignKeyModifyRule UNKNOWN = new DBSForeignKeyModifyRule("UNKNOWN", "?", null); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBSForeignKeyModifyRule NO_ACTION = new DBSForeignKeyModifyRule("NO_ACTION", ModelMessages.model_struct_No_Action, null); //$NON-NLS-1$
    public static final DBSForeignKeyModifyRule CASCADE = new DBSForeignKeyModifyRule("CASCADE", ModelMessages.model_struct_Cascade, "CASCADE"); //$NON-NLS-1$ //$NON-NLS-3$
    public static final DBSForeignKeyModifyRule SET_NULL = new DBSForeignKeyModifyRule("SET_NULL", ModelMessages.model_struct_Set_NULL, "SET NULL"); //$NON-NLS-1$ //$NON-NLS-3$
    public static final DBSForeignKeyModifyRule SET_DEFAULT = new DBSForeignKeyModifyRule("SET_DEFAULT", ModelMessages.model_struct_Set_Default, "SET DEFAULT"); //$NON-NLS-1$ //$NON-NLS-3$
    public static final DBSForeignKeyModifyRule RESTRICT = new DBSForeignKeyModifyRule("RESTRICT", ModelMessages.model_struct_Restrict, "RESTRICT"); //$NON-NLS-1$ //$NON-NLS-3$

    private final String id;
    private final String name;
    private final String clause;

    public DBSForeignKeyModifyRule(String id, String name, String clause)
    {
        this.id = id;
        this.name = name;
        this.clause = clause;
    }

    public String getId()
    {
        return id;
    }

    @NotNull
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
