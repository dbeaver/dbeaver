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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.sqlite.SQLiteExtensionLoading;
import org.jkiss.dbeaver.ext.sqlite.SQLiteExtensionPermissionStore;
import org.jkiss.dbeaver.ext.sqlite.SQLiteExtensionSettings;
import org.jkiss.dbeaver.ext.sqlite.ui.internal.SQLiteUIMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.IDataSourceConnectionEditorSite;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.IConnectionWizard;

public class SQLiteExtensionsSettings extends Composite {
    private final IDataSourceConnectionEditorSite site;
    private final Button enabled;
    private boolean changed;
    private Runnable selectionListener = () -> { };

    public SQLiteExtensionsSettings(@NotNull Composite parent, @NotNull IDataSourceConnectionEditorSite site) {
        super(parent, SWT.NONE);
        this.site = site;
        setLayout(new GridLayout(1, false));
        setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        enabled = UIUtils.createCheckbox(this, SQLiteUIMessages.extension_settings_enable,
            SQLiteUIMessages.extension_settings_warning, false, 1);
        enabled.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
            changed = true;
            saveSettings(site.getActiveDataSource());
            selectionListener.run();
            site.updateButtons();
        }));
        ((IConnectionWizard) site.getWizard()).addPropertyChangeListener(event -> {
            if (!isDisposed() && (IConnectionWizard.PROP_CONNECTION_SAVED.equals(event.getProperty())
                || IConnectionWizard.PROP_CONNECTION_TEST.equals(event.getProperty()))
                && event.getNewValue() instanceof DBPDataSourceContainer container
                && container.getDriver() == site.getDriver()) {
                try {
                    SQLiteExtensionPermissionStore.getInstance().invalidate(SQLiteExtensionSettings.capture(container));
                } catch (DBException exception) {
                    showError(exception);
                }
            }
        });
        loadSettings();
    }

    public void setSelectionListener(@NotNull Runnable listener) {
        selectionListener = listener;
    }

    public boolean isLoadingEnabled() {
        return enabled.getSelection();
    }

    public void loadSettings() {
        if (!changed) {
            try {
                enabled.setSelection(SQLiteExtensionSettings.capture(site.getActiveDataSource()).enabled());
            } catch (DBException exception) {
                showError(exception);
            }
        }
    }

    public void saveSettings(@NotNull DBPDataSourceContainer container) {
        if (changed) {
            container.getConnectionConfiguration().setProviderProperty(
                SQLiteExtensionLoading.PROP_ENABLED, Boolean.toString(enabled.getSelection()));
        }
    }

    private void showError(@NotNull DBException exception) {
        DBWorkbench.getPlatformUI().showError(SQLiteUIMessages.extension_confirmation_title,
            SQLiteUIMessages.extension_settings_error, exception);
    }
}
