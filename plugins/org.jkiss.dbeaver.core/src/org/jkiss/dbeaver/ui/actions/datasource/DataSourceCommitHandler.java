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
package org.jkiss.dbeaver.ui.actions.datasource;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.runtime.TasksJob;
import org.jkiss.dbeaver.ui.actions.AbstractDataSourceHandler;

import java.lang.reflect.InvocationTargetException;

public class DataSourceCommitHandler extends AbstractDataSourceHandler
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

    public static void execute(@NotNull final DBCExecutionContext context) {
        TasksJob.runTask("Commit transaction", new DBRRunnableWithProgress() {
            @Override
            public void run(DBRProgressMonitor monitor)
                throws InvocationTargetException, InterruptedException {
                DBCTransactionManager txnManager = DBUtils.getTransactionManager(context);
                if (txnManager != null) {
                    try (DBCSession session = context.openSession(monitor, DBCExecutionPurpose.UTIL, "Commit transaction")) {
                        txnManager.commit(session);
                    } catch (DBCException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            }
        });
    }

}