/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
 * Copyright (C) 2019 Andrew Khitrin (ahitrin@gmail.com)
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
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTablespace;
import org.jkiss.dbeaver.ext.postgresql.ui.PostgreCreateTablespaceDialog;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.Map;


public class PostgreTablespaceConfigurator implements DBEObjectConfigurator<PostgreTablespace> {

    @Override
    public PostgreTablespace configureObject(@NotNull DBRProgressMonitor monitor, @Nullable DBECommandContext commandContext, @Nullable Object container, @NotNull PostgreTablespace tablespace, @NotNull Map<String, Object> options) {
        return new UITask<PostgreTablespace>() {
            @Override
            protected PostgreTablespace runTask() {
                PostgreCreateTablespaceDialog dialog = new PostgreCreateTablespaceDialog(UIUtils.getActiveWorkbenchShell(), tablespace);
                if (dialog.open() != IDialogConstants.OK_ID) {
                    return null;
                }
                tablespace.setName(dialog.getName());
                tablespace.setLoc(dialog.getLoc());
                tablespace.setOwnerId(dialog.getOwner().getObjectId());
                tablespace.setOptions(dialog.getOptions());
                return tablespace;
            }
        }.execute();
    }

}
