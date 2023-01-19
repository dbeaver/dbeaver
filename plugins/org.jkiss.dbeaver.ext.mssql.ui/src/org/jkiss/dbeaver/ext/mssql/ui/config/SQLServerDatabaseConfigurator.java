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
package org.jkiss.dbeaver.ext.mssql.ui.config;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerDatabase;
import org.jkiss.dbeaver.ext.mssql.ui.SQLServerCreateDatabaseDialog;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.Map;

public class SQLServerDatabaseConfigurator implements DBEObjectConfigurator<SQLServerDatabase> {

    @Override
    public SQLServerDatabase configureObject(DBRProgressMonitor monitor, Object container, SQLServerDatabase database, Map<String, Object> options) {
        return UITask.run(() -> {
            SQLServerCreateDatabaseDialog dialog = new SQLServerCreateDatabaseDialog(UIUtils.getActiveWorkbenchShell(), database.getDataSource());

            if (dialog.open() != IDialogConstants.OK_ID) {
                return null;
            }

            database.setName(dialog.getName());
            return database;
        });
    }
}
