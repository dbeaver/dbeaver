/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.exasol.ui.config;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.jkiss.dbeaver.ext.exasol.model.security.ExasolRole;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.Map;

public class ExasolRoleConfigurator implements DBEObjectConfigurator<ExasolRole> {
    @Override
    public ExasolRole configureObject(DBRProgressMonitor monitor, Object container, ExasolRole role, Map<String, Object> options) {
        return new UITask<ExasolRole>() {
            @Override
            protected ExasolRole runTask() {
                ExasolRoleDialog dialog = new ExasolRoleDialog(UIUtils.getActiveWorkbenchShell());
                if (dialog.open() != IDialogConstants.OK_ID) {
                    return null;
                }

                role.setName(dialog.getName());
                role.setDescription(dialog.getDescription());
                return role;
            }
        }.execute();
    }
}
