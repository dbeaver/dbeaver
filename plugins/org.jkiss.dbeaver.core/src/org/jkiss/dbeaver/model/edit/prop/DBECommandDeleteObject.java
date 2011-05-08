/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.edit.prop;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.edit.DBECommand;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;

import java.util.Map;

/**
 * Delete object command
 */
public abstract class DBECommandDeleteObject<OBJECT_TYPE extends DBPObject> extends DBECommandAbstract<OBJECT_TYPE> {

    //public static final String PROP_COMPOSITE_COMMAND = ".composite";

    public DBECommandDeleteObject(OBJECT_TYPE object, String title)
    {
        super(object, title);
    }

    @Override
    public DBECommand<?> merge(DBECommand<?> prevCommand, Map<Object, Object> userParams)
    {
        if (prevCommand.getObject() == getObject()) {
            return null;
        }
        return this;
    }

}