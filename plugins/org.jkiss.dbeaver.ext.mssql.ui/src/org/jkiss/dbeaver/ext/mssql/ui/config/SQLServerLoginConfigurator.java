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
package org.jkiss.dbeaver.ext.mssql.ui.config;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mssql.SQLServerUtils;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerLogin;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerWindowsAccount;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.List;
import java.util.Map;

public class SQLServerLoginConfigurator implements DBEObjectConfigurator<SQLServerLogin> {
    private static final Log log = Log.getLog(SQLServerLoginConfigurator.class);

    @Nullable
    @Override
    public SQLServerLogin configureObject(
        @NotNull DBRProgressMonitor monitor,
        @Nullable DBECommandContext commandContext,
        @Nullable Object container,
        @NotNull SQLServerLogin login,
        @NotNull Map<String, Object> options
    ) {
        DBPDriver driver = login.getDataSource().getContainer().getDriver();
        boolean windowsSupported = !SQLServerUtils.isDriverAzure(driver) && !SQLServerUtils.isDriverFabric(driver)
            && !SQLServerUtils.isDriverBabelfish(driver);
        List<String> knownDomains = List.of();
        if (windowsSupported) {
            try {
                knownDomains = SQLServerWindowsAccount.readKnownDomains(monitor, login.getDataSource());
            } catch (DBException e) {
                log.debug("Cannot suggest Windows login domains", e);
            }
        }
        List<String> domains = knownDomains;
        return new UITask<SQLServerLogin>() {
            @Override
            @Nullable
            protected SQLServerLogin runTask() {
                SQLServerLoginDialog dialog = new SQLServerLoginDialog(UIUtils.getActiveWorkbenchShell(), login, windowsSupported, domains);
                return dialog.open() == IDialogConstants.OK_ID ? login : null;
            }
        }.execute();
    }
}
