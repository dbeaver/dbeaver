/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.edit;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.edit.DBECommand;

import java.util.Map;

/**
 * Abstract object command
 */
public class DBECommandAbstract<OBJECT_TYPE extends DBPObject> implements DBECommand<OBJECT_TYPE> {
    private final OBJECT_TYPE object;
    private final String title;

    public DBECommandAbstract(OBJECT_TYPE object, String title)
    {
        this.object = object;
        this.title = title;
    }

    @Override
    public OBJECT_TYPE getObject()
    {
        return object;
    }

    @Override
    public String getTitle()
    {
        return title;
    }

    @Override
    public boolean isUndoable()
    {
        return true;
    }

    @Override
    public void validateCommand() throws DBException
    {
        // do nothing by default
    }

    @Override
    public void updateModel()
    {
    }

    @Override
    public DBECommand<?> merge(DBECommand<?> prevCommand, Map<Object, Object> userParams)
    {
        return this;
    }

    @Override
    public IDatabasePersistAction[] getPersistActions()
    {
        return null;
    }

}
