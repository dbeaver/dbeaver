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
package org.jkiss.dbeaver.ui.actions.datasource;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCTransactionManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.DataSourceHandler;

import java.lang.reflect.InvocationTargetException;

public class DataSourceCommitHandler extends DataSourceHandler
{
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        DBSDataSourceContainer dataSourceContainer = getDataSourceContainer(event, true);
        if (dataSourceContainer != null && dataSourceContainer.isConnected()) {
            execute(HandlerUtil.getActiveShell(event), dataSourceContainer);
        }
        return null;
    }

    public static void execute(Shell shell, @NotNull final DBSDataSourceContainer dataSourceContainer) {
        try {
            DBeaverUI.runInProgressService(new DBRRunnableWithProgress() {
                @Override
                public void run(DBRProgressMonitor monitor)
                    throws InvocationTargetException, InterruptedException
                {
                    DBPDataSource dataSource = dataSourceContainer.getDataSource();
                    if (dataSource != null) {
                        DBCTransactionManager txnManager = DBUtils.getTransactionManager(dataSource);
                        if (txnManager != null) {
                            DBCSession session = dataSource.openSession(monitor, DBCExecutionPurpose.UTIL, "Commit transaction");
                            try {
                                txnManager.commit(session);
                            } catch (DBCException e) {
                                throw new InvocationTargetException(e);
                            } finally {
                                session.close();
                            }
                        }
                    }
                }
            });
        } catch (InvocationTargetException e) {
            UIUtils.showErrorDialog(shell, "Commit", "Error while committing session", e);
        } catch (InterruptedException e) {
            // do nothing
        }
    }

}