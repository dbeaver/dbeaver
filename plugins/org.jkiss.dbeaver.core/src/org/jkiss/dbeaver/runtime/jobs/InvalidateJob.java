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
package org.jkiss.dbeaver.runtime.jobs;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * InvalidateJob
 */
public class InvalidateJob extends DataSourceJob
{
    private long timeSpent;
    private DBCExecutionContext.InvalidateResult invalidateResult;
    private Exception invalidateError;

    public InvalidateJob(
        DBPDataSource dataSource)
    {
        super("Invalidate " + dataSource.getContainer().getName(), null, dataSource);
    }

    public DBCExecutionContext.InvalidateResult getInvalidateResult() {
        return invalidateResult;
    }

    public Exception getInvalidateError() {
        return invalidateError;
    }

    public long getTimeSpent() {
        return timeSpent;
    }

    @Override
    protected IStatus run(DBRProgressMonitor monitor)
    {
        try {
            // Close datasource
            monitor.subTask("Invalidate datasource");
            long startTime = System.currentTimeMillis();
            try {
                invalidateResult = getExecutionContext().invalidateContext(monitor);
            } finally {
                timeSpent = System.currentTimeMillis() - startTime;
            }

        }
        catch (Exception ex) {
            invalidateError = ex;
        }
        return Status.OK_STATUS;
    }

    @Override
    protected void canceling()
    {
        getThread().interrupt();
    }

}