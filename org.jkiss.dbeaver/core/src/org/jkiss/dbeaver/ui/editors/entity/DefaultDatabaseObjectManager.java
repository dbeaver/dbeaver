/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.entity;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabaseObjectManager;

/**
 * DefaultDatabaseObjectManager
 */
public class DefaultDatabaseObjectManager extends AbstractDatabaseObjectManager<DBSObject> {

    public DefaultDatabaseObjectManager() {
    }

    @Override
    protected void executePersistAction(DBCExecutionContext context, IDatabasePersistAction action) throws DBException
    {
        throw new DBException("Object persistence is not implemented");
    }

}
