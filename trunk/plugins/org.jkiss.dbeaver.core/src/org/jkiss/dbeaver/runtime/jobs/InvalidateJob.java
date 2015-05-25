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
package org.jkiss.dbeaver.runtime.jobs;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
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
        DBCExecutionContext context)
    {
        super("Invalidate " + context.getDataSource().getContainer().getName(), null, context);
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