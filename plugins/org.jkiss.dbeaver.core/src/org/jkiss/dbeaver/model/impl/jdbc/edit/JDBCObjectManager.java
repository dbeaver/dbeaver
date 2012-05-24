/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.edit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommand;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.impl.edit.AbstractObjectManager;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * JDBCObjectManager
 */
public abstract class JDBCObjectManager<OBJECT_TYPE extends DBSObject> extends AbstractObjectManager<OBJECT_TYPE> {

    protected static final Log log = LogFactory.getLog(JDBCObjectManager.class);

    @Override
    public void executePersistAction(DBCExecutionContext context, DBECommand<OBJECT_TYPE> command, IDatabasePersistAction action) throws DBException
    {
        String script = action.getScript();

        DBCStatement dbStat = DBUtils.createStatement(context, script);
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
