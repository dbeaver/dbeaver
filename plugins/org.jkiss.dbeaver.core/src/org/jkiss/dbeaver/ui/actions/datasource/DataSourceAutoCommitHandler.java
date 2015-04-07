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
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPTransactionIsolation;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCTransactionManager;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.actions.DataSourceHandler;

import java.util.Map;

public class DataSourceAutoCommitHandler extends DataSourceHandler implements IElementUpdater
{
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        DBSDataSourceContainer dataSourceContainer = getDataSourceContainer(event, true);
        if (dataSourceContainer != null) {
            boolean newAutocommit = !dataSourceContainer.isDefaultAutoCommit();
            if (dataSourceContainer.isConnected()) {
                // Get flag from connection
                newAutocommit = !dataSourceContainer.isConnectionAutoCommit();
            }
            dataSourceContainer.setDefaultAutoCommit(newAutocommit, true);
            dataSourceContainer.persistConfiguration();
        }
        return null;
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
            DBCTransactionManager txnManager = DBUtils.getTransactionManager(dataSource);
            if (txnManager != null) {
                try {
                    // Change auto-commit mode
                    boolean autoCommit = txnManager.isAutoCommit();
                    element.setChecked(autoCommit);
                    // Update command image
                    element.setIcon(autoCommit ? DBIcon.TXN_COMMIT_AUTO.getImageDescriptor() : DBIcon.TXN_COMMIT_MANUAL.getImageDescriptor());
                    DBPTransactionIsolation isolation = txnManager.getTransactionIsolation();
                    String isolationName = isolation == null ? "?" : isolation.getTitle();
                    element.setText(autoCommit ? "Switch to manual commit (" + isolationName + ")" : "Switch to auto-commit");
                    element.setTooltip(autoCommit ? "Auto-commit" : "Manual commit (" + isolationName + ")");
                } catch (DBCException e) {
                    log.warn(e);
                }
            }
        }
    }
}