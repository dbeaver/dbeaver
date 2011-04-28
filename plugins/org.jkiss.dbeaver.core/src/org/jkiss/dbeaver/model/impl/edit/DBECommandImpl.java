/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
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
public class DBECommandImpl<OBJECT_TYPE extends DBPObject> implements DBECommand<OBJECT_TYPE> {
    private final OBJECT_TYPE object;
    private final String title;

    public DBECommandImpl(OBJECT_TYPE object, String title)
    {
        this.object = object;
        this.title = title;
    }

    public OBJECT_TYPE getObject()
    {
        return object;
    }

    public String getTitle()
    {
        return title;
    }

    public boolean isUndoable()
    {
        return true;
    }

    public void validateCommand() throws DBException
    {
        // do nothing by default
    }

    public void updateModel()
    {
    }

    public DBECommand<?> merge(DBECommand<?> prevCommand, Map<String, Object> userParams)
    {
        return this;
    }

    public IDatabasePersistAction[] getPersistActions()
    {
        return null;
    }

}
