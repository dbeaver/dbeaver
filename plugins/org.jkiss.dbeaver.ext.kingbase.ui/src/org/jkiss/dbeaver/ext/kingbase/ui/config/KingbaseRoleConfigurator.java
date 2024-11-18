/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

package org.jkiss.dbeaver.ext.kingbase.ui.config;

import java.util.Map;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseRole;
import org.jkiss.dbeaver.ext.kingbase.ui.KingbaseCreateRoleDialog;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * Kingbase role configurator
 */
public class KingbaseRoleConfigurator implements DBEObjectConfigurator<KingbaseRole> {
    @Override
    public KingbaseRole configureObject(@NotNull DBRProgressMonitor monitor, @Nullable DBECommandContext commandContext, @Nullable Object parent, @NotNull KingbaseRole role, @NotNull Map<String, Object> options) {
        return new UITask<KingbaseRole>() {
            @Override
            protected KingbaseRole runTask() {
            	KingbaseCreateRoleDialog dialog = new KingbaseCreateRoleDialog(UIUtils.getActiveWorkbenchShell(), role);
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
