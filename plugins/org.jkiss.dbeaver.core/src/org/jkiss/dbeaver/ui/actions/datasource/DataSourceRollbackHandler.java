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
package org.jkiss.dbeaver.ui.actions.datasource;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.runtime.TasksJob;
import org.jkiss.dbeaver.ui.actions.AbstractDataSourceHandler;

import java.lang.reflect.InvocationTargetException;

public class DataSourceRollbackHandler extends AbstractDataSourceHandler
{
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        DBCExecutionContext context = getExecutionContext(event, true);
        if (context != null && context.isConnected()) {
            execute(context);
        }
        return null;
    }

    public static void execute(final DBCExecutionContext context) {
        TasksJob.runTask("Rollback transaction", new DBRRunnableWithProgress() {
            @Override
            public void run(DBRProgressMonitor monitor)
                throws InvocationTargetException, InterruptedException {
                DBCTransactionManager txnManager = DBUtils.getTransactionManager(context);
                if (txnManager != null) {
                    try (DBCSession session = context.openSession(monitor, DBCExecutionPurpose.UTIL, "Rollback transaction")) {
                        txnManager.rollback(session, null);
                    } catch (DBCException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            }
        });
    }

}