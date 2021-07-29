/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.access.DBAPasswordChangeInfo;
import org.jkiss.dbeaver.model.access.DBAUserChangePassword;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;

import java.lang.reflect.InvocationTargetException;

public class ChangePasswordDialogHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent executionEvent) throws ExecutionException {
        final ISelection selection = HandlerUtil.getCurrentSelection(executionEvent);
        if (!(selection instanceof IStructuredSelection)) {
            return null;
        }
        final DBNNode node = NavigatorUtils.getSelectedNode(selection);
        if (node instanceof DBNDataSource) {
            DBNDataSource dataSource = (DBNDataSource) node;
            DBPDataSourceContainer dataSourceContainer = dataSource.getDataSourceContainer();
            DBPDataSource dbpDataSource = dataSourceContainer.getDataSource();
            if (dbpDataSource instanceof IAdaptable) {
                DBAUserChangePassword changePassword = ((IAdaptable) dbpDataSource).getAdapter(DBAUserChangePassword.class);
                if (changePassword != null) {
                    DBPConnectionConfiguration connectionInfo = dataSourceContainer.getConnectionConfiguration();
                    String oldPassword = connectionInfo.getUserPassword();
                    String userName = connectionInfo.getUserName();
                    DBAPasswordChangeInfo userPassword = DBWorkbench.getPlatformUI().promptUserPasswordChange(UIConnectionMessages.dialog_user_password_change_label, userName, oldPassword, false, false);
                    if (userPassword != null) {
                        String newPassword = userPassword.getNewPassword();
                        try {
                            UIUtils.runInProgressService(monitor -> {
                                try {
                                    changePassword.changeUserPassword(monitor, userName, newPassword, oldPassword);

                                    if (DBWorkbench.getPlatformUI().confirmAction(
                                        UIConnectionMessages.dialog_user_password_change_question_label,
                                        UIConnectionMessages.dialog_user_password_change_question_message)) {
                                        connectionInfo.setUserPassword(newPassword);
                                        dataSourceContainer.getRegistry().flushConfig();
                                    }
                                } catch (DBException e) {
                                    DBWorkbench.getPlatformUI().showError("Change user password", "Password change error for user: " + userName, e);
                                }
                            });
                        } catch (InvocationTargetException e) {
                            // skip
                        } catch (InterruptedException e) {
                            DBWorkbench.getPlatformUI().showError("Change user password", "User password change error", e);
                        }
                    }
                }
            }
        }

        return null;
    }
}
