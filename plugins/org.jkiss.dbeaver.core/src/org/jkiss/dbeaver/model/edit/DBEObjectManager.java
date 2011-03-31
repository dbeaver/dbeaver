/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.edit;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;

/**
 * DBEObjectManager
 */
public interface DBEObjectManager<OBJECT_TYPE extends DBPObject> {

    void executePersistAction(
        DBCExecutionContext context,
        DBECommand<OBJECT_TYPE> command,
        IDatabasePersistAction action)
        throws DBException;

}