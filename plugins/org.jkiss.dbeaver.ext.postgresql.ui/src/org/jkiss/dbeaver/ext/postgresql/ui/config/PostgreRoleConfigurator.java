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

package org.jkiss.dbeaver.ext.postgresql.ui.config;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreRole;
import org.jkiss.dbeaver.ext.postgresql.ui.PostgreCreateRoleDialog;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * Postgre role configurator
 */
public class PostgreRoleConfigurator implements DBEObjectConfigurator<PostgreRole> {
    @Override
    public PostgreRole configureObject(DBRProgressMonitor monitor, Object parent, PostgreRole role) {
        return new UITask<PostgreRole>() {
            @Override
            protected PostgreRole runTask() {
                PostgreCreateRoleDialog dialog = new PostgreCreateRoleDialog(UIUtils.getActiveWorkbenchShell(), role);
                if (dialog.open() != IDialogConstants.OK_ID) {
                    return null;
                }
                role.setName(dialog.getName());
                role.setPassword(dialog.getPassword());
                role.setCanLogin(dialog.isUser());
                return role;
            }
        }.execute();
    }

}
