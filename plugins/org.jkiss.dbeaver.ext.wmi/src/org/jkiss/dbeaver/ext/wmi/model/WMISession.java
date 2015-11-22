/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.wmi.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.impl.AbstractSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * Execution context
 */
public class WMISession extends AbstractSession {

    private final WMIDataSource dataSource;

    public WMISession(DBRProgressMonitor monitor, DBCExecutionPurpose purpose, String taskTitle, WMIDataSource dataSource)
    {
        super(monitor, purpose, taskTitle);
        this.dataSource = dataSource;
    }

    @NotNull
    @Override
    public DBCExecutionContext getExecutionContext() {
        return dataSource;
    }

    @NotNull
    @Override
    public WMIDataSource getDataSource()
    {
        return dataSource;
    }

    @NotNull
    @Override
    public DBCStatement prepareStatement(@NotNull DBCStatementType type, @NotNull String query, boolean scrollable, boolean updatable, boolean returnGeneratedKeys) throws DBCException
    {
        return new WMIStatement(this, type, query);
    }

    @Override
    public void cancelBlock() throws DBException
    {
        // Cancel WMI async call
    }

}
