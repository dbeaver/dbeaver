/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
