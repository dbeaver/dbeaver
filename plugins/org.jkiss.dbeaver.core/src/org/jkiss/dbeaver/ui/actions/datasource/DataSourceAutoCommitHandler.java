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
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPTransactionIsolation;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCTransactionManager;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.AbstractDataSourceHandler;

import java.util.Map;

public class DataSourceAutoCommitHandler extends AbstractDataSourceHandler implements IElementUpdater
{
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        DBCExecutionContext context = getExecutionContext(event, true);
        if (context != null) {
            DBCTransactionManager txnManager = DBUtils.getTransactionManager(context);
            if (txnManager != null) {
                try {
                    DBSDataSourceContainer container = context.getDataSource().getContainer();
                    boolean newAutocommit = !container.isDefaultAutoCommit();
                    if (context.isConnected()) {
                        // Get flag from connection
                        newAutocommit = !txnManager.isAutoCommit();
                    }
                    container.setDefaultAutoCommit(newAutocommit, context, true);
                    container.persistConfiguration();
                } catch (DBException e) {
                    UIUtils.showErrorDialog(null, "Auto-Commit", "Error while toggle auto-commit", e);
                }
            }
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
        DBCExecutionContext context = getExecutionContext(activeEditor);
        if (context != null && context.isConnected()) {
            DBCTransactionManager txnManager = DBUtils.getTransactionManager(context);
            if (txnManager != null) {
                try {
                    // Change auto-commit mode
                    boolean autoCommit = txnManager.isAutoCommit();
                    element.setChecked(autoCommit);
                    // Update command image
                    element.setIcon(DBeaverIcons.getImageDescriptor(autoCommit ? UIIcon.TXN_COMMIT_AUTO : UIIcon.TXN_COMMIT_MANUAL));
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