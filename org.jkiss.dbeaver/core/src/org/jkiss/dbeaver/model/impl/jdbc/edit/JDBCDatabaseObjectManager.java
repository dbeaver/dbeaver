/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.edit;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabaseObjectManager;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * JDBC DatabaseObjectManager implementation
 */
public abstract class JDBCDatabaseObjectManager<OBJECT_TYPE extends DBSObject> extends AbstractDatabaseObjectManager<OBJECT_TYPE> {

    @Override
    protected void executePersistAction(DBCExecutionContext context, IDatabasePersistAction action, boolean undo) throws DBException
    {
        String script = undo ? action.getUndoScript() : action.getScript();

        DBCStatement dbStat = context.prepareStatement(script, false, false, false);
        try {
            dbStat.executeStatement();
        } finally {
            dbStat.close();
        }
    }
}
