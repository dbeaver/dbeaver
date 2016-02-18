/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.model.impl.edit;

import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Script command
 */
public class SQLScriptCommand<OBJECT_TYPE extends DBSObject> extends DBECommandAbstract<OBJECT_TYPE> {

    private String script;

    public SQLScriptCommand(OBJECT_TYPE object, String title, String script)
    {
        super(object, title);
        this.script = script;
    }

    @Override
    public void updateModel()
    {
    }

    @Override
    public DBEPersistAction[] getPersistActions()
    {
        return new DBEPersistAction[] {
            new SQLDatabasePersistAction(
                getTitle(),
                script)
        };
    }

}