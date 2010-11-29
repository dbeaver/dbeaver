/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.runtime;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.model.MySQLUser;
import org.jkiss.dbeaver.model.impl.jdbc.edit.JDBCDatabaseObjectManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * MySQLUserManager
 */
public class MySQLUserManager extends JDBCDatabaseObjectManager<MySQLUser> {

    @Override
    public boolean supportsEdit() {
        return true;
    }

    @Override
    public void saveChanges(DBRProgressMonitor monitor) throws DBException {
        super.saveChanges(monitor);
    }

    @Override
    public void resetChanges(DBRProgressMonitor monitor) {
        super.resetChanges(monitor);
    }

}
