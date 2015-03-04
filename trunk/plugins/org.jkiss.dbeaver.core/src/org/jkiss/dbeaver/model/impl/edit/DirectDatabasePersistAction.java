/*
 * Copyright (C) 2010-2015 Serge Rieder
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
package org.jkiss.dbeaver.model.impl.edit;

import org.jkiss.dbeaver.model.edit.DBEPersistAction;

/**
 * Direct persist action implementation
 */
public abstract class DirectDatabasePersistAction implements DBEPersistAction {

    private final String title;
    private final ActionType type;

    public DirectDatabasePersistAction(String title)
    {
        this(title, ActionType.NORMAL);
    }

    public DirectDatabasePersistAction(String title, ActionType type)
    {
        this.title = title;
        this.type = type;
    }

    @Override
    public String getTitle()
    {
        return title;
    }

    @Override
    public String getScript()
    {
        return null;
    }

    @Override
    public ActionType getType()
    {
        return type;
    }

}
