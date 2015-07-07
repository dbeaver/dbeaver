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
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.ArrayList;
import java.util.List;

/**
 * InvalidateJob
 */
public class InvalidateJob extends DataSourceJob
{
    public static class ContextInvalidateResult {
        public final DBCExecutionContext.InvalidateResult result;
        public final Exception error;

        public ContextInvalidateResult(DBCExecutionContext.InvalidateResult result, Exception error) {
            this.result = result;
            this.error = error;
        }
    }

    private long timeSpent;
    private List<ContextInvalidateResult> invalidateResults = new ArrayList<ContextInvalidateResult>();

    public InvalidateJob(
        DBCExecutionContext context)
    {
        super("Invalidate " + context.getDataSource().getContainer().getName(), null, context);
    }

    public List<ContextInvalidateResult> getInvalidateResults() {
        return invalidateResults;
    }

    public long getTimeSpent() {
        return timeSpent;
    }

    @Override
    protected IStatus run(DBRProgressMonitor monitor)
    {
        DBPDataSource dataSource = getExecutionContext().getDataSource();
        // Invalidate datasource
        monitor.subTask("Invalidate datasource [" + dataSource.getContainer().getName() + "]");
        List<DBCExecutionContext> allContexts = new ArrayList<DBCExecutionContext>(dataSource.getAllContexts());
        for (DBCExecutionContext context : allContexts) {
            long startTime = System.currentTimeMillis();
            try {
                invalidateResults.add(new ContextInvalidateResult(context.invalidateContext(monitor), null));
            } catch (Exception e) {
                invalidateResults.add(new ContextInvalidateResult(DBCExecutionContext.InvalidateResult.ERROR, e));
            } finally {
                timeSpent += (System.currentTimeMillis() - startTime);
            }
        }

        return Status.OK_STATUS;
    }

    @Override
    protected void canceling()
    {
        getThread().interrupt();
    }

}