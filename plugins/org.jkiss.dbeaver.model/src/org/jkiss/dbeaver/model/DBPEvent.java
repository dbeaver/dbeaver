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

package org.jkiss.dbeaver.model;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * DBPEvent
 */
public class DBPEvent
{
    public enum Action
    {
        OBJECT_ADD,
        OBJECT_UPDATE,
        OBJECT_REMOVE,
        OBJECT_SELECT,
    }

    private Action action;

    private DBSObject object;
    private Boolean enabled;
    private Object data;

    public DBPEvent(Action action, DBSObject object) {
        this.action = action;
        this.object = object;
    }

    public DBPEvent(Action action, DBSObject object, boolean enabled) {
        this.action = action;
        this.object = object;
        this.enabled = enabled;
    }

    public DBPEvent(Action action, DBSObject object, @Nullable Object data) {
        this.action = action;
        this.object = object;
        this.data = data;
    }

    public Action getAction()
    {
        return action;
    }

    public DBSObject getObject() {
        return object;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public Object getData() {
        return data;
    }

}
