/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.edit;

import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabaseObjectCommand;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Composite object command
 */
public abstract class DatabaseObjectCompositeCommand<OBJECT_TYPE extends DBSObject, HANDLER_TYPE extends DatabaseObjectPropertyHandler<OBJECT_TYPE>>
    extends AbstractDatabaseObjectCommand<OBJECT_TYPE> {

    private Map<HANDLER_TYPE, Object> properties = new HashMap<HANDLER_TYPE, Object>();

    protected DatabaseObjectCompositeCommand(String title, Image icon)
    {
        super(title, icon);
    }

    protected DatabaseObjectCompositeCommand(String title)
    {
        super(title);
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
    public Object merge(IDatabaseObjectCommand<OBJECT_TYPE> prevCommand, Map<String, Object> userParams)
    {
        return null;
    }

    @Override
    public void validateCommand(OBJECT_TYPE object) throws DBException
    {
    }

    public void updateModel(OBJECT_TYPE object)
    {
    }

}