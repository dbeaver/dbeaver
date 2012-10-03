/*
 * Copyright (C) 2010-2012 Serge Rieder
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
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
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
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.DataSourceHandler;
import org.jkiss.dbeaver.ui.preferences.PrefConstants;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public class DataSourceAutoCommitHandler extends DataSourceHandler implements IElementUpdater
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

    public static void execute(Shell shell, final DBSDataSourceContainer dataSourceContainer) {
        final DBPDataSource dataSource = dataSourceContainer.getDataSource();
        try {
            DBeaverCore.getInstance().runInProgressService(new DBRRunnableWithProgress() {
                @Override
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
                    } catch (DBCException e) {
                        throw new InvocationTargetException(e);
                    } finally {
                        context.close();
                    }
                }
            });
        } catch (InvocationTargetException e) {
            UIUtils.showErrorDialog(shell, "Auto-Commit", "Error while toggle auto-commit", e.getTargetException());
        } catch (InterruptedException e) {
            // do nothing
        }
    }

    @Override
    public void updateElement(UIElement element, Map parameters)
    {
        IWorkbenchWindow workbenchWindow = (IWorkbenchWindow) element.getServiceLocator().getService(IWorkbenchWindow.class);
        if (workbenchWindow == null || workbenchWindow.getActivePage() == null) {
            return;
        }
        IEditorPart activeEditor = workbenchWindow.getActivePage().getActiveEditor();
        if (activeEditor == null) {
            return;
        }
        DBSDataSourceContainer dataSourceContainer = getDataSourceContainer(activeEditor);
        if (dataSourceContainer != null && dataSourceContainer.isConnected()) {
            final DBPDataSource dataSource = dataSourceContainer.getDataSource();
            DBCExecutionContext context = dataSource.openContext(
                VoidProgressMonitor.INSTANCE,
                DBCExecutionPurpose.UTIL,
                "Get autocommit mode");
            try {
                DBCTransactionManager txnManager = context.getTransactionManager();
                // Change auto-commit mode
                boolean autoCommit = txnManager.isAutoCommit();
                element.setChecked(autoCommit);
                // Update command image
                element.setIcon(autoCommit ? DBIcon.TXN_COMMIT_AUTO.getImageDescriptor() : DBIcon.TXN_COMMIT_MANUAL.getImageDescriptor());
                element.setText(autoCommit ? "Switch to manual commit mode" : "Switch to auto commit mode");
            } catch (DBCException e) {
                log.warn(e);
            } finally {
                context.close();
            }
        }
    }
}