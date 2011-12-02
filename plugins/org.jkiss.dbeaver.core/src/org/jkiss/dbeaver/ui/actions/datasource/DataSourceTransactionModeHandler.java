/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.datasource;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCTransactionManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.DataSourceHandler;
import org.jkiss.dbeaver.ui.preferences.PrefConstants;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public class DataSourceTransactionModeHandler extends DataSourceHandler implements IElementUpdater
{
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        DBSDataSourceContainer dataSourceContainer = getDataSourceContainer(event, true, false);
        if (dataSourceContainer != null && dataSourceContainer.isConnected()) {
            execute(HandlerUtil.getActiveShell(event), dataSourceContainer);
        }
        return null;
    }

    public static void execute(Shell shell, final DBSDataSourceContainer dataSourceContainer) {
        final DBPDataSource dataSource = dataSourceContainer.getDataSource();
        try {
            DBeaverCore.getInstance().runInProgressService(new DBRRunnableWithProgress() {
                public void run(DBRProgressMonitor monitor)
                    throws InvocationTargetException, InterruptedException
                {
                    DBCExecutionContext context = dataSource.openContext(monitor, DBCExecutionPurpose.UTIL, "Change '" + dataSourceContainer.getName() + "' transactional mode");
                    try {
                        DBCTransactionManager txnManager = context.getTransactionManager();
                        // Change auto-commit mode
                        boolean newAutoCommit = !txnManager.isAutoCommit();
                        txnManager.setAutoCommit(newAutoCommit);

                        // Update data source settings
                        IPreferenceStore preferenceStore = dataSourceContainer.getPreferenceStore();
                        preferenceStore.setValue(PrefConstants.DEFAULT_AUTO_COMMIT, newAutoCommit);
                        dataSourceContainer.getRegistry().flushConfig();

                        // Update command image
                    } catch (DBCException e) {
                        throw new InvocationTargetException(e);
                    } finally {
                        context.close();
                    }
                }
            });
        } catch (InvocationTargetException e) {
            UIUtils.showErrorDialog(shell, "Auto-Commit", "Error while toggle auto-commit", e);
        } catch (InterruptedException e) {
            // do nothing
        }
    }

    public void updateElement(UIElement element, Map parameters)
    {
        IWorkbenchWindow workbenchWindow = (IWorkbenchWindow) element.getServiceLocator().getService(IWorkbenchWindow.class);
        if (workbenchWindow == null || workbenchWindow.getActivePage() == null) {
            return;
        }
        DBSDataSourceContainer dataSourceContainer = getDataSourceContainer(workbenchWindow.getActivePage().getActivePart());
        if (dataSourceContainer != null && dataSourceContainer.isConnected()) {
            final DBPDataSource dataSource = dataSourceContainer.getDataSource();
            DBCExecutionContext context = dataSource.openContext(
                VoidProgressMonitor.INSTANCE,
                DBCExecutionPurpose.UTIL,
                "Get autocommit mode");
            try {
                DBCTransactionManager txnManager = context.getTransactionManager();
                // Change auto-commit mode
                element.setChecked(txnManager.isAutoCommit());
                // Update command image
            } catch (DBCException e) {
                log.warn(e);
            } finally {
                context.close();
            }
        }
    }
}