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
package org.jkiss.dbeaver.ext.mssql.ui.config;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerLogin;
import org.jkiss.dbeaver.ext.mssql.ui.SQLServerUIMessages;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.EditUserDialog;

public class SQLServerLoginConfigurator implements DBEObjectConfigurator<SQLServerLogin> {

    @Override
    public SQLServerLogin configureObject(DBRProgressMonitor monitor, Object container, SQLServerLogin login) {
        return new UITask<SQLServerLogin>() {
            @Override
            protected SQLServerLogin runTask() {
                EditUserDialog dialog = new EditUserDialog(UIUtils.getActiveWorkbenchShell(), SQLServerUIMessages.dialog_create_login_shell_title);
                if (dialog.open() != IDialogConstants.OK_ID) {
                    return null;
                }
                login.setLoginName(dialog.getName());
                login.setPassword(dialog.getPassword());
                return login;
            }
        }.execute();
    }
}
