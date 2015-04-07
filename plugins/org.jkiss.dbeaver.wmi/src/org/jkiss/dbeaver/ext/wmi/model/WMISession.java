/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.wmi.model;

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

    @Override
    public DBCExecutionContext getExecutionContext() {
        return dataSource;
    }

    @Override
    public WMIDataSource getDataSource()
    {
        return dataSource;
    }

    @Override
    public DBCStatement prepareStatement(DBCStatementType type, String query, boolean scrollable, boolean updatable, boolean returnGeneratedKeys) throws DBCException
    {
        return new WMIStatement(this, type, query);
    }

    @Override
    public void cancelBlock() throws DBException
    {
        // Cancel WMI async call
    }

}
