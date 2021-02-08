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

package org.jkiss.dbeaver.ext.postgresql.ui.config;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.ui.PostgreCreateDatabaseDialog;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * Postgre database configurator
 */
public class PostgreDatabaseConfigurator implements DBEObjectConfigurator<PostgreDatabase> {

    @Override
    public PostgreDatabase configureObject(DBRProgressMonitor monitor, Object dataSource, PostgreDatabase database) {
        return new UITask<PostgreDatabase>() {
            @Override
            protected PostgreDatabase runTask() {
                PostgreCreateDatabaseDialog dialog = new PostgreCreateDatabaseDialog(UIUtils.getActiveWorkbenchShell(), database.getDataSource());
                if (dialog.open() != IDialogConstants.OK_ID) {
                    return null;
                }
                database.setName(dialog.getName());
                database.setInitialOwner(dialog.getOwner());
                database.setTemplateName(dialog.getTemplateName());
                database.setInitialTablespace(dialog.getTablespace());
                database.setInitialEncoding(dialog.getEncoding());
                return database;
            }
        }.execute();
    }
}
