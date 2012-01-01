/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.wmi.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.impl.AbstractExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * Execution context
 */
public class WMIExecutionContext extends AbstractExecutionContext {

    private final WMIDataSource dataSource;

    public WMIExecutionContext(DBRProgressMonitor monitor, DBCExecutionPurpose purpose, String taskTitle, WMIDataSource dataSource)
    {
        super(monitor, purpose, taskTitle);
        this.dataSource = dataSource;
    }

    public DBPDataSource getDataSource()
    {
        return dataSource;
    }

    public DBCStatement prepareStatement(DBCStatementType type, String query, boolean scrollable, boolean updatable, boolean returnGeneratedKeys) throws DBCException
    {
        return null;
    }

    public void cancelBlock() throws DBException
    {
        // Cancel WMI async call
    }

}
