/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.cdata.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.cdata.model.CDataConnectionHierarchy;
import org.jkiss.dbeaver.ext.cdata.model.CDataConnectionHierarchyReader;
import org.jkiss.dbeaver.ext.cdata.registry.CDataDriverDescriptor;
import org.jkiss.dbeaver.ext.cdata.ui.internal.CDataUIMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.access.DBAAuthModel;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.DatabaseNativeAuthModelConfigurator;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicReference;

public class CDataAuthModelConfigurator extends DatabaseNativeAuthModelConfigurator {
    private static final Log log = Log.getLog(CDataAuthModelConfigurator.class);

    private CDataConnectionHierarchy hierarchy;
    private CDataConnectionEditor editor;
    private Label errorLabel;
    private Link retryLink;
    private Runnable changeListener;
    private String loadError;

    @Override
    public void createControl(
        @NotNull Composite parent,
        @NotNull DBAAuthModel<?> model,
        @NotNull Runnable propertyChangeListener
    ) {
        if (!credentialsPromptMode) {
            super.createControl(parent, model, propertyChangeListener);
            return;
        }
        changeListener = propertyChangeListener;
        Composite panel = UIUtils.createPlaceholder(parent, 1);
        int columns = parent.getLayout() instanceof GridLayout layout ? layout.numColumns : 1;
        panel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, columns, 1));
        Composite general = UIUtils.createFormPlaceholder(panel, 2, 1);
        UIUtils.setControlVisible(general, false);
        final Composite credentials = UIUtils.createFormPlaceholder(panel, 2, 1);
        savePasswordCheck = UIUtils.createCheckbox(panel,
            UIConnectionMessages.dialog_connection_wizard_final_checkbox_save_password, false);
        savePasswordCheck.setEnabled(canEditCredentialsPerPolicy);
        savePasswordCheck.addListener(SWT.Selection, event -> changeListener.run());
        editor = new CDataConnectionEditor(general, credentials, property -> updateValidation());
        editor.setCredentialsPromptMode(true);
        editor.setPasswordOptionsControl(savePasswordCheck);
        errorLabel = new Label(panel, SWT.WRAP);
        GridData errorData = new GridData(GridData.FILL_HORIZONTAL);
        errorData.widthHint = 355;
        errorLabel.setLayoutData(errorData);
        retryLink = new Link(panel, SWT.NONE);
        retryLink.setText(CDataUIMessages.connection_editor_retry);
        retryLink.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        retryLink.addListener(SWT.Selection, event -> {
            loadSettings(dataSource);
            UIUtils.resizeShell(panel.getShell());
        });
    }

    @Override
    public void loadSettings(@NotNull DBPDataSourceContainer dataSource) {
        if (!credentialsPromptMode) {
            super.loadSettings(dataSource);
            return;
        }
        this.dataSource = dataSource;
        hierarchy = null;
        loadError = null;
        AtomicReference<CDataConnectionHierarchy> loaded = new AtomicReference<>();
        try {
            UIUtils.runInProgressService(monitor -> {
                monitor.beginTask(CDataUIMessages.connection_editor_loading, 1);
                try {
                    loaded.set(CDataConnectionHierarchyReader.read(monitor, (CDataDriverDescriptor) dataSource.getDriver()));
                    if (monitor.isCanceled()) {
                        throw new InterruptedException();
                    }
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                } finally {
                    monitor.done();
                }
            });
            CDataConnectionHierarchy result = loaded.get();
            result.loadConfiguration(getDriver().getDriverInfo().jdbcName(), dataSource.getConnectionConfiguration());
            hierarchy = result;
        } catch (InvocationTargetException | DBException e) {
            log.debug("Unable to load CData credential fields", e);
            loadError = CDataUIMessages.connection_credentials_unavailable;
        } catch (InterruptedException expected) {
            loadError = CDataUIMessages.connection_credentials_unavailable;
        }
        savePasswordCheck.setSelection(canEditCredentialsPerPolicy && dataSource.isSavePassword());
        editor.setHierarchy(hierarchy);
        updateValidation();
    }

    @Override
    public void saveSettings(@NotNull DBPDataSourceContainer dataSource) {
        if (!credentialsPromptMode) {
            super.saveSettings(dataSource);
            return;
        }
        if (!isComplete()) {
            return;
        }
        try {
            // the credentials dialog caller restores the saved configuration when storage is disabled
            hierarchy.saveCredentialValues(getDriver().getDriverInfo().jdbcName(), dataSource.getConnectionConfiguration());
            dataSource.setSavePassword(canEditCredentialsPerPolicy && savePasswordCheck.getSelection());
        } catch (DBException e) {
            log.debug("Unable to apply CData credentials", e);
            loadError = CDataUIMessages.connection_editor_invalid_url;
            updateValidation();
        }
    }

    @Override
    public boolean isComplete() {
        return !credentialsPromptMode || hierarchy != null && getErrorMessage() == null;
    }

    @Nullable
    @Override
    public String getErrorMessage() {
        return loadError != null ? loadError : editor == null ? null : editor.getValidationError();
    }

    private void updateValidation() {
        String error = getErrorMessage();
        errorLabel.setText(error == null ? "" : error);
        UIUtils.setControlVisible(errorLabel, error != null);
        UIUtils.setControlVisible(retryLink, loadError != null);
        errorLabel.getParent().layout(true, true);
        changeListener.run();
    }

    @NotNull
    private CDataDriverDescriptor getDriver() {
        return (CDataDriverDescriptor) dataSource.getDriver();
    }
}
