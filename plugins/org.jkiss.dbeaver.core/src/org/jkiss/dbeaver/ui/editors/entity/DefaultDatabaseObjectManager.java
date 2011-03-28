/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.entity;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.model.edit.DBECommand;
import org.jkiss.dbeaver.model.edit.DBEObjectManager;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * DefaultDatabaseObjectManager
 */
public class DefaultDatabaseObjectManager implements DBEObjectManager<DBSObject> {

    public void executePersistAction(DBCExecutionContext context, DBECommand<DBSObject> dbsObjectDBECommand, IDatabasePersistAction action) throws DBException
    {
        throw new DBException("Persistence not supported in default object manager");
    }
}
