/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.edit;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.impl.edit.DBOEditorImpl;
import org.jkiss.dbeaver.model.impl.edit.DBOManagerImpl;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.utils.ContentUtils;

/**
 * JDBC DatabaseObjectManager implementation
 */
public abstract class DBOEditorJDBC<OBJECT_TYPE extends DBSObject> extends DBOEditorImpl<OBJECT_TYPE> {

    @Override
    protected void executePersistAction(DBCExecutionContext context, IDatabasePersistAction action) throws DBException
    {
        String script = action.getScript();

        DBCStatement dbStat = context.prepareStatement(script, false, false, false);
        try {
            dbStat.executeStatement();
            action.handleExecute(null);
        } catch (DBCException e) {
            action.handleExecute(e);
            throw new DBCException("Could not execute script:" + ContentUtils.getDefaultLineSeparator() + script, e);
        } finally {
            dbStat.close();
        }
    }
}
