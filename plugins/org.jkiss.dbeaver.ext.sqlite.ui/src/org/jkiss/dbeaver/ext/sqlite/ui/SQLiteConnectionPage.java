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
package org.jkiss.dbeaver.ext.sqlite.ui;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.views.GenericConnectionPage;
import org.jkiss.dbeaver.ext.sqlite.SQLiteExtensionPermissionStore;
import org.jkiss.dbeaver.ext.sqlite.SQLiteExtensionSettings;
import org.jkiss.dbeaver.ext.sqlite.ui.internal.SQLiteUIMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.IDataSourceConnectionEditorSite;
import org.jkiss.dbeaver.ui.dialogs.IConnectionWizard;

public class SQLiteConnectionPage extends GenericConnectionPage {
    @Override
    public void setSite(@NotNull IDataSourceConnectionEditorSite site) {
        super.setSite(site);
        if (site.getWizard() instanceof IConnectionWizard wizard) {
            wizard.addPropertyChangeListener(event -> {
                if ((IConnectionWizard.PROP_CONNECTION_SAVED.equals(event.getProperty())
                    || IConnectionWizard.PROP_CONNECTION_TEST.equals(event.getProperty()))
                    && event.getNewValue() instanceof DBPDataSourceContainer container
                    && container.getDriver() == site.getDriver()) {
                    try {
                        SQLiteExtensionPermissionStore.getInstance().invalidate(SQLiteExtensionSettings.capture(container));
                    } catch (DBException exception) {
                        DBWorkbench.getPlatformUI().showError(SQLiteUIMessages.extension_confirmation_title,
                            SQLiteUIMessages.extension_settings_error, exception);
                    }
                }
            });
        }
    }
}
