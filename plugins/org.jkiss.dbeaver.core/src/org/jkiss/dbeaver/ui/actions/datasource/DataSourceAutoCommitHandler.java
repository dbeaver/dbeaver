/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPTransactionIsolation;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.IDataSourceContainerProvider;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCTransactionManager;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.actions.AbstractDataSourceHandler;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.util.Map;

public class DataSourceAutoCommitHandler extends AbstractDataSourceHandler implements IElementUpdater {
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        DBCExecutionContext context = getActiveExecutionContext(event, true);
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
                    boolean autoCommit = newAutocommit;
                    new AbstractJob("Set auto-commit") {
                        @Override
                        protected IStatus run(DBRProgressMonitor monitor) {
                            monitor.beginTask("Change connection auto-commit to " + autoCommit, 1);
                            try {
                                monitor.subTask("Change context '" + context.getContextName() + "' auto-commit state");
                                txnManager.setAutoCommit(monitor, autoCommit);
                            } catch (Exception e) {
                                return GeneralUtils.makeExceptionStatus(e);
                            } finally {
                                monitor.done();
                            }
                            return Status.OK_STATUS;
                        }
                    }.schedule();
                } catch (DBException e) {
                    DBWorkbench.getPlatformUI().showError("Auto-Commit", "Error while toggle auto-commit", e);
                }
            }
        }
        return null;
    }

    @Override
    public void updateElement(UIElement element, Map parameters) {
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
        DBCExecutionContext context = getExecutionContextFromPart(activeEditor);
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
        String text = autoCommit ? NLS.bind(CoreMessages.action_menu_transaction_manualcommit_name, isolationName) : CoreMessages.action_menu_transaction_autocommit_name;
        element.setText(text);
        element.setTooltip(text);
    }
}