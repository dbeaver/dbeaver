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
package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.access.DBAAuthModel;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.impl.auth.AuthModelDatabaseShellCommandCredentials;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.TextWithOpenFolder;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;
import org.jkiss.utils.CommonUtils;

/**
 * Auth model config for "password from shell command".
 *
 * Renders a username field and a multi-line command field (no manual password / save-password
 * controls). The command is persisted as an auth property; selecting this model forces
 * save-password semantics so the connect flow never prompts.
 */
public class ShellCommandAuthModelConfigurator extends DatabaseNativeAuthModelConfigurator {

    private Text commandText;
    private TextWithOpenFolder workingDirectory;
    private Spinner timeoutSpinner;

    @Override
    public void createControl(@NotNull Composite authPanel, @Nullable DBAAuthModel<?> object, @NotNull Runnable propertyChangeListener) {
        // Base renders the username only (this model reports password as not applicable)
        super.createControl(authPanel, object, propertyChangeListener);

        Label commandLabel = UIUtils.createControlLabel(authPanel, UIConnectionMessages.dialog_connection_auth_shell_command_label);
        commandLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

        commandText = new Text(authPanel, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gd.heightHint = UIUtils.getFontHeight(authPanel) * 5;
        gd.widthHint = UIUtils.getFontHeight(authPanel) * 30;
        commandText.setLayoutData(gd);
        commandText.addModifyListener(e -> propertyChangeListener.run());

        // Text.setMessage() does not render on SWT.MULTI on any platform, so paint a hint manually.
        final String hintText = UIConnectionMessages.dialog_connection_auth_shell_command_hint;
        commandText.addPaintListener(e -> {
            if (commandText.getCharCount() > 0) {
                return;
            }
            e.gc.setForeground(commandText.getDisplay().getSystemColor(SWT.COLOR_WIDGET_DISABLED_FOREGROUND));
            e.gc.drawText(hintText, 3, 0, true);
        });

        UIUtils.createControlLabel(authPanel, UIConnectionMessages.dialog_connection_auth_shell_command_workdir_label);
        workingDirectory = new TextWithOpenFolder(authPanel, null);
        workingDirectory.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        workingDirectory.getTextControl().addModifyListener(e -> propertyChangeListener.run());

        timeoutSpinner = UIUtils.createLabelSpinner(authPanel,
            UIConnectionMessages.dialog_connection_auth_shell_command_timeout_label,
            AuthModelDatabaseShellCommandCredentials.DEFAULT_TIMEOUT_MS, 1000, 3_600_000);
        timeoutSpinner.setIncrement(1000);
        timeoutSpinner.setPageIncrement(10000);
        timeoutSpinner.addModifyListener(e -> propertyChangeListener.run());
    }

    @Override
    public void loadSettings(@NotNull DBPDataSourceContainer dataSource) {
        super.loadSettings(dataSource);
        DBPConnectionConfiguration configuration = dataSource.getConnectionConfiguration();
        if (commandText != null && !commandText.isDisposed()) {
            commandText.setText(CommonUtils.notEmpty(
                configuration.getAuthProperty(AuthModelDatabaseShellCommandCredentials.PROP_COMMAND)));
        }
        if (workingDirectory != null && !workingDirectory.getTextControl().isDisposed()) {
            workingDirectory.setText(CommonUtils.notEmpty(
                configuration.getAuthProperty(AuthModelDatabaseShellCommandCredentials.PROP_WORKING_DIR)));
        }
        if (timeoutSpinner != null && !timeoutSpinner.isDisposed()) {
            timeoutSpinner.setSelection(CommonUtils.toInt(
                configuration.getAuthProperty(AuthModelDatabaseShellCommandCredentials.PROP_TIMEOUT),
                AuthModelDatabaseShellCommandCredentials.DEFAULT_TIMEOUT_MS));
        }
    }

    @Override
    public void saveSettings(@NotNull DBPDataSourceContainer dataSource) {
        super.saveSettings(dataSource);
        if (commandText != null) {
            String cmd = commandText.getText();
            dataSource.getConnectionConfiguration().setAuthProperty(
                AuthModelDatabaseShellCommandCredentials.PROP_COMMAND,
                CommonUtils.isEmptyTrimmed(cmd) ? null : cmd);
        }
        if (workingDirectory != null) {
            String dir = workingDirectory.getText();
            dataSource.getConnectionConfiguration().setAuthProperty(
                AuthModelDatabaseShellCommandCredentials.PROP_WORKING_DIR,
                CommonUtils.isEmptyTrimmed(dir) ? null : dir);
        }
        if (timeoutSpinner != null) {
            dataSource.getConnectionConfiguration().setAuthProperty(
                AuthModelDatabaseShellCommandCredentials.PROP_TIMEOUT,
                String.valueOf(timeoutSpinner.getSelection()));
        }
        // No password is stored; force save-password semantics so the connect flow runs the
        // command instead of prompting.
        dataSource.setSavePassword(true);
    }

    @Override
    public boolean isComplete() {
        return commandText != null && !CommonUtils.isEmptyTrimmed(commandText.getText());
    }
}
