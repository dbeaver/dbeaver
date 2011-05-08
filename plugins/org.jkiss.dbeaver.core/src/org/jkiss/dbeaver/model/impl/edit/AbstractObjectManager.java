/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.edit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.model.DBPEvent;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommand;
import org.jkiss.dbeaver.model.edit.DBECommandReflector;
import org.jkiss.dbeaver.model.edit.DBEObjectManager;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Abstract object manager
 */
public abstract class AbstractObjectManager<OBJECT_TYPE extends DBSObject> implements DBEObjectManager<OBJECT_TYPE> {

    protected static final Log log = LogFactory.getLog(AbstractObjectManager.class);

    public static class CreateObjectReflector<OBJECT_TYPE extends DBSObject> implements DBECommandReflector<OBJECT_TYPE, DBECommand<OBJECT_TYPE>> {

        public void redoCommand(DBECommand<OBJECT_TYPE> command)
        {
            command.getObject().getDataSource().getContainer().fireEvent(new DBPEvent(DBPEvent.Action.OBJECT_ADD, command.getObject()));
        }

        public void undoCommand(DBECommand<OBJECT_TYPE> command)
        {
            command.getObject().getDataSource().getContainer().fireEvent(new DBPEvent(DBPEvent.Action.OBJECT_REMOVE, command.getObject()));
        }
    }

    public static class DeleteObjectReflector<OBJECT_TYPE extends DBSObject> implements DBECommandReflector<OBJECT_TYPE, DBECommand<OBJECT_TYPE>> {

        public void redoCommand(DBECommand<OBJECT_TYPE> command)
        {
            command.getObject().getDataSource().getContainer().fireEvent(new DBPEvent(DBPEvent.Action.OBJECT_REMOVE, command.getObject()));
        }

        public void undoCommand(DBECommand<OBJECT_TYPE> command)
        {
            command.getObject().getDataSource().getContainer().fireEvent(new DBPEvent(DBPEvent.Action.OBJECT_ADD, command.getObject()));
        }

    }

}
