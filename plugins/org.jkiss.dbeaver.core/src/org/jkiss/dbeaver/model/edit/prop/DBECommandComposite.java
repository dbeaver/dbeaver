/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.edit.prop;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.edit.DBECommand;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;

import java.util.HashMap;
import java.util.Map;

/**
 * Composite object command
 */
public abstract class DBECommandComposite<OBJECT_TYPE extends DBPObject, HANDLER_TYPE extends DBEPropertyHandler<OBJECT_TYPE>>
    extends DBECommandAbstract<OBJECT_TYPE> {

    public static final String PROP_COMPOSITE_COMMAND = ".composite";

    private Map<HANDLER_TYPE, Object> properties = new HashMap<HANDLER_TYPE, Object>();

    protected DBECommandComposite(OBJECT_TYPE object, String title)
    {
        super(object, title);
    }

    public Map<HANDLER_TYPE, Object> getProperties()
    {
        return properties;
    }

    public Object getProperty(HANDLER_TYPE handler)
    {
        return properties.get(handler);
    }

    public void addPropertyHandler(HANDLER_TYPE handler, Object value)
    {
        properties.put(handler, value);
    }

    @Override
    public void validateCommand() throws DBException
    {
    }

    public void updateModel()
    {
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