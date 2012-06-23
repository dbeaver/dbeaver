/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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
