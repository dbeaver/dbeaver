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

package org.jkiss.dbeaver.ext.gaussdb.ui.config;

import java.util.Map;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.gaussdb.model.GaussDBDatabase;
import org.jkiss.dbeaver.ext.gaussdb.ui.GaussDBCreateDatabaseDialog;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;

public class GaussDBDatabaseConfigurator implements DBEObjectConfigurator<GaussDBDatabase> {

    @Override
    public GaussDBDatabase configureObject(@NotNull DBRProgressMonitor monitor, @Nullable DBECommandContext commandContext,
        @Nullable Object container, @NotNull GaussDBDatabase database, @NotNull Map<String, Object> options) {
        return new UITask<GaussDBDatabase>() {
            @Override
            protected GaussDBDatabase runTask() throws DBException {
                GaussDBCreateDatabaseDialog dialog = new GaussDBCreateDatabaseDialog(UIUtils.getActiveWorkbenchShell(),
                    database.getDataSource());
                if (dialog.open() != IDialogConstants.OK_ID) {
                    return null;
                }
                database.setName(dialog.getName());
                database.setInitialOwner(dialog.getOwner());
                database.setTemplateName(dialog.getTemplateName());
                database.setInitialTablespace(dialog.getTablespace());
                database.setInitialEncoding(dialog.getEncoding());
                database.setDatabaseCompatibleMode(dialog.getCompatibleMode());
                return database;
            }
        }.execute();
    }
}
