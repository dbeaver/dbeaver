/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.edit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommand;
import org.jkiss.dbeaver.model.edit.DBECommandReflector;
import org.jkiss.dbeaver.model.edit.DBEObjectManager;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Abstract object manager
 */
public abstract class AbstractObjectManager<OBJECT_TYPE extends DBSObject> implements DBEObjectManager<OBJECT_TYPE> {

    protected static final Log log = LogFactory.getLog(AbstractObjectManager.class);

    public static class CreateObjectReflector<OBJECT_TYPE extends DBSObject> implements DBECommandReflector<OBJECT_TYPE, DBECommand<OBJECT_TYPE>> {

        @Override
        public void redoCommand(DBECommand<OBJECT_TYPE> command)
        {
            DBUtils.fireObjectAdd(command.getObject());
        }

        @Override
        public void undoCommand(DBECommand<OBJECT_TYPE> command)
        {
            DBUtils.fireObjectRemove(command.getObject());
        }
    }

    public static class DeleteObjectReflector<OBJECT_TYPE extends DBSObject> implements DBECommandReflector<OBJECT_TYPE, DBECommand<OBJECT_TYPE>> {

        @Override
        public void redoCommand(DBECommand<OBJECT_TYPE> command)
        {
            DBUtils.fireObjectRemove(command.getObject());
        }

        @Override
        public void undoCommand(DBECommand<OBJECT_TYPE> command)
        {
            DBUtils.fireObjectAdd(command.getObject());
        }

    }

}
