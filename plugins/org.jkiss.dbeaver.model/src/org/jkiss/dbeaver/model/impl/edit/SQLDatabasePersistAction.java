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

/**
 * Object persist action implementation
 */
public class SQLDatabasePersistAction implements DBEPersistAction {

    private final String title;
    private final String script;
    private final ActionType type;

    public SQLDatabasePersistAction(String title, String script)
    {
        this(title, script, ActionType.NORMAL);
    }

    public SQLDatabasePersistAction(String title, String script, ActionType type)
    {
        this.title = title;
        this.script = script;
        this.type = type;
    }

    public SQLDatabasePersistAction(String script)
    {
        this("", script, ActionType.NORMAL);
    }

    @Override
    public String getTitle()
    {
        return title;
    }

    @Override
    public String getScript()
    {
        return script;
    }

    @Override
    public void handleExecute(Throwable error)
    {
        // do nothing
    }

    @Override
    public ActionType getType()
    {
        return type;
    }

}
