/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.access.DBAUserPasswordManager;
import org.jkiss.dbeaver.model.access.DBAuthUtils;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.registry.ApplicationPolicyProvider;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;

import java.lang.reflect.InvocationTargetException;

public class ChangePasswordDialogHandler extends AbstractHandler {

    private static final Log log = Log.getLog(ChangePasswordDialogHandler.class);

    @Override
    @Nullable
    public Object execute(@NotNull ExecutionEvent executionEvent) throws ExecutionException {
        ISelection selection = HandlerUtil.getCurrentSelection(executionEvent);
        if (selection == null) {
            return null;
        }
        DBNNode node = NavigatorUtils.getSelectedNode(selection);
        if (!(node instanceof DBNDataSource dataSource)) {
            return null;
        }
        DBPDataSourceContainer container = dataSource.getDataSourceContainer();
        if (!DBAuthUtils.canChangeCurrentUserPassword(container)) {
            return null;
        }
        DBPDataSource originalDataSource = container.getDataSource();
        DBAUserPasswordManager passwordManager = DBUtils.getAdapter(DBAUserPasswordManager.class, originalDataSource);
        if (passwordManager == null) {
            return null;
        }
        DBPConnectionConfiguration originalConfiguration = new DBPConnectionConfiguration(container.getConnectionConfiguration());
        DBPConnectionConfiguration originalActualConfiguration =
            new DBPConnectionConfiguration(container.getActualConnectionConfiguration());
        try {
            UIUtils.runInProgressService(monitor -> DBAuthUtils.promptAndChangePasswordForCurrentUser(monitor, container,
                (passwordMonitor, userName, newPassword, oldPassword) -> {
                    if (container.getDataSource() != originalDataSource
                        || container.getRegistry().getDataSource(container.getId()) != container
                        || !DBAuthUtils.canChangeCurrentUserPassword(container)
                        || !originalConfiguration.equals(container.getConnectionConfiguration())
                        || !originalActualConfiguration.equals(container.getActualConnectionConfiguration())) {
                        throw new DBException("The connection or password change permissions have changed. Reopen the dialog.");
                    }
                    passwordManager.changeUserPassword(passwordMonitor, userName, newPassword, oldPassword);
                }, !ApplicationPolicyProvider.getInstance().isPolicyEnabled(ApplicationPolicyProvider.POLICY_CREDENTIALS_EDIT)));
        } catch (InvocationTargetException e) {
            log.error(e.getTargetException());
        } catch (InterruptedException e) {
            DBWorkbench.getPlatformUI().showError(UIConnectionMessages.dialog_password_change_title,
                UIConnectionMessages.dialog_password_change_error, e);
        }
        return null;
    }
}
