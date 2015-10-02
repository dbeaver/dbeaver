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

package org.jkiss.dbeaver.model.edit.prop;

import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;

import java.util.HashMap;
import java.util.Map;

/**
 * Composite object command
 */
public abstract class DBECommandComposite<OBJECT_TYPE extends DBPObject, HANDLER_TYPE extends DBEPropertyHandler<OBJECT_TYPE>>
    extends DBECommandAbstract<OBJECT_TYPE> {

    private Map<Object, Object> properties = new HashMap<>();

    protected DBECommandComposite(OBJECT_TYPE object, String title)
    {
        super(object, title);
    }

    public Map<Object, Object> getProperties()
    {
        return properties;
    }

    public Object getProperty(Object id)
    {
        return properties.get(id);
    }

    public Object getProperty(HANDLER_TYPE handler)
    {
        return properties.get(handler.getId());
    }

    public void addPropertyHandler(HANDLER_TYPE handler, Object value)
    {
        properties.put(handler.getId(), value);
    }

//    public static <OBJECT_TYPE extends DBPObject> DBECommandComposite getFromContext(DBECommandContext context, OBJECT_TYPE object, DBEPropertyHandler<OBJECT_TYPE> handler)
//    {
//        String compositeName = object.toString() + PROP_COMPOSITE_COMMAND;
//        DBECommandComposite compositeCommand = (DBECommandComposite)context.getUserParams().get(compositeName);
//        if (compositeCommand == null) {
//            compositeCommand = handler.createCompositeCommand(object);
//            context.getUserParams().put(compositeName, compositeCommand);
//        }
//        return compositeCommand;
//    }

}