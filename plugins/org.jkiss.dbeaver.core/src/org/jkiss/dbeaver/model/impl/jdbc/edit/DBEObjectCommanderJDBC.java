/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.edit;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.impl.edit.DBEObjectCommanderImpl;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * JDBC DatabaseObjectManager implementation
 */
public abstract class DBEObjectCommanderJDBC<OBJECT_TYPE extends DBSObject> extends DBEObjectCommanderImpl<OBJECT_TYPE> {

    @Override
    protected void executePersistAction(DBCExecutionContext context, IDatabasePersistAction action) throws DBException
    {
        String script = action.getScript();

        DBCStatement dbStat = DBUtils.prepareStatement(context, script);
        try {
            dbStat.executeStatement();
            action.handleExecute(null);
        } catch (DBCException e) {
            action.handleExecute(e);
            throw e;
        } finally {
            dbStat.close();
        }
    }
}
