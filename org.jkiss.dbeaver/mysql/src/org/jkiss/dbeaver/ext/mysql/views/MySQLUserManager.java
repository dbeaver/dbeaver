/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.views;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.model.MySQLUser;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.editors.AbstractDatabaseObjectManager;

/**
 * MySQLUserManager
 */
public class MySQLUserManager extends AbstractDatabaseObjectManager<MySQLUser> {
    @Override
    public boolean supportsEdit() {
        return super.supportsEdit();
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
