/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.runtime.jobs;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.net.DBWNetworkHandler;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.io.IOException;
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
    private List<ContextInvalidateResult> invalidateResults = new ArrayList<>();
    //private boolean reconnect;

    public InvalidateJob(
        DBCExecutionContext context/*,
        boolean reconnect*/)
    {
        super("Invalidate " + context.getDataSource().getContainer().getName(), null, context);
//        this.reconnect = reconnect;
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
        DBPDataSourceContainer container = dataSource.getContainer();
        DBWNetworkHandler[] activeHandlers = container.getActiveNetworkHandlers();
        boolean networkOK = true;
        if (activeHandlers != null && activeHandlers.length > 0) {
            for (DBWNetworkHandler nh : activeHandlers) {
                monitor.subTask("Invalidate network [" + container.getName() + "]");
                try {
                    nh.invalidateHandler(monitor);
                } catch (Exception e) {
                    invalidateResults.add(new ContextInvalidateResult(DBCExecutionContext.InvalidateResult.ERROR, e));
                    networkOK = false;
                    break;
                }
            }
        }
        if (networkOK) {
            // Invalidate datasource
            monitor.subTask("Invalidate connections of [" + container.getName() + "]");
            DBCExecutionContext[] allContexts = dataSource.getAllContexts();
            for (DBCExecutionContext context : allContexts) {
                long startTime = System.currentTimeMillis();
                try {
                    final DBCExecutionContext.InvalidateResult result = context.invalidateContext(monitor);
                    invalidateResults.add(new ContextInvalidateResult(result, null));
                } catch (Exception e) {
                    invalidateResults.add(new ContextInvalidateResult(DBCExecutionContext.InvalidateResult.ERROR, e));
                } finally {
                    timeSpent += (System.currentTimeMillis() - startTime);
                }
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