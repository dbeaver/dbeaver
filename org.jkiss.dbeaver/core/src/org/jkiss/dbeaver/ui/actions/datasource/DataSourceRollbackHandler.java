/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.datasource;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.dbc.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.ui.actions.DataSourceHandler;
import org.jkiss.dbeaver.utils.DBeaverUtils;

import java.lang.reflect.InvocationTargetException;

public class DataSourceRollbackHandler extends DataSourceHandler
{
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        final DBSDataSourceContainer dataSourceContainer = getDataSourceContainer(event, false);
        if (dataSourceContainer != null && dataSourceContainer.isConnected()) {
            execute(HandlerUtil.getActiveShell(event), dataSourceContainer);
        }
        return null;
    }

    public static void execute(Shell shell, final DBSDataSourceContainer dataSourceContainer) {
        final DBPDataSource dataSource = dataSourceContainer.getDataSource();
        try {
            DBeaverCore.getInstance().runAndWait2(new DBRRunnableWithProgress() {
                public void run(DBRProgressMonitor monitor)
                    throws InvocationTargetException, InterruptedException
                {
                    DBCExecutionContext context = dataSource.openContext(monitor, "Rollback '" + dataSourceContainer.getName() + "' transaction");
                    try {
                        context.getTransactionManager().rollback(null);
                    }
                    catch (DBCException e) {
                        throw new InvocationTargetException(e);
                    }
                    finally {
                        context.close();
                    }
                }
            });
        } catch (InvocationTargetException e) {
            DBeaverUtils.showErrorDialog(shell, "Rollback", "Error during session rollback", e);
        } catch (InterruptedException e) {
            // do nothing
        }
    }

}