/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.edit.prop;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.impl.edit.DBECommandImpl;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Composite object command
 */
public abstract class DBECommandComposite<OBJECT_TYPE extends DBPObject, HANDLER_TYPE extends DBEPropertyHandler<OBJECT_TYPE>>
    extends DBECommandImpl<OBJECT_TYPE> {

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

}