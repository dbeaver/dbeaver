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
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPTransactionIsolation;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.IDataSourceContainerProvider;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCTransactionManager;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
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
                    final DBPDataSourceContainer container = context.getDataSource().getContainer();
                    boolean newAutocommit = !container.isDefaultAutoCommit();
                    if (context.isConnected()) {
                        // Get flag from connection
                        newAutocommit = !txnManager.isAutoCommit();
                    }
                    container.setDefaultAutoCommit(newAutocommit, context, true, new Runnable() {
                        @Override
                        public void run() {
                            // Save config
                            container.persistConfiguration();
                        }
                    });
                } catch (DBException e) {
                    DBUserInterface.getInstance().showError("Auto-Commit", "Error while toggle auto-commit", e);
                }
            }
        }
        return null;
    }

    @Override
    public void updateElement(UIElement element, Map parameters)
    {
        IWorkbenchWindow workbenchWindow = element.getServiceLocator().getService(IWorkbenchWindow.class);
        if (workbenchWindow == null || workbenchWindow.getActivePage() == null) {
            return;
        }
        IEditorPart activeEditor = workbenchWindow.getActivePage().getActiveEditor();
        if (activeEditor == null) {
            return;
        }

        boolean autoCommit = true;
        DBPTransactionIsolation isolation = null;
        DBCExecutionContext context = getExecutionContext(activeEditor);
        if (context != null && context.isConnected()) {
            DBCTransactionManager txnManager = DBUtils.getTransactionManager(context);
            if (txnManager != null) {
                try {
                    // Change auto-commit mode
                    autoCommit = txnManager.isAutoCommit();
                    isolation = txnManager.getTransactionIsolation();
                } catch (DBCException e) {
                    log.warn(e);
                }
            }
        } else if (activeEditor instanceof IDataSourceContainerProvider) {
            DBPDataSourceContainer container = ((IDataSourceContainerProvider) activeEditor).getDataSourceContainer();
            if (container != null) {
                autoCommit = container.isDefaultAutoCommit();
                isolation = container.getActiveTransactionsIsolation();
            }
        }
        element.setChecked(autoCommit);
        // Update command image
        element.setIcon(DBeaverIcons.getImageDescriptor(autoCommit ? UIIcon.TXN_COMMIT_AUTO : UIIcon.TXN_COMMIT_MANUAL));
        String isolationName = isolation == null ? "?" : isolation.getTitle();
        element.setText(autoCommit ? "Switch to manual commit (" + isolationName + ")" : "Switch to auto-commit");
        element.setTooltip(autoCommit ? "Manual commit (" + isolationName + ")" : "Auto-commit");
    }
}