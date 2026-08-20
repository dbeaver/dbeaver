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
package org.jkiss.dbeaver.ui.services;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.preferences.ConfirmedShellCommandsStore;
import org.jkiss.dbeaver.model.runtime.DBRShellCommand;
import org.jkiss.dbeaver.model.runtime.RunnableWithResult;
import org.jkiss.dbeaver.runtime.ui.UIServiceShellCommands;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;

import java.util.Map;

public class UIServiceShellCommandsImpl implements UIServiceShellCommands {

    private final ConfirmedShellCommandsStore confirmedCommandsStore = ConfirmedShellCommandsStore.getInstance();

    @Override
    public boolean isShellCommandExecutionEnabled() {
        return true;
    }

    @Override
    public boolean addConfirmedCommand(@NotNull DBRShellCommand command) throws DBException {
        return !command.isBlank() && confirmedCommandsStore.add(command.getCommand());
    }

    @Override
    public void validateByUser(@NotNull DBRShellCommand command, @NotNull Map<String, String> approvalContext) throws DBException {
        if (command.isBlank()) {
            return;
        }
        boolean isApprovedByUser = confirmedCommandsStore.contains(command.getCommand())
            || askApproveForCommand(command, approvalContext);
        if (!isApprovedByUser) {
            throw new DBException(NLS.bind(
                CoreMessages.shell_cmd_manager_add_command_error_message,
                String.join(", ", approvalContext.values())
            ));
        }
    }

    private boolean askApproveForCommand(@NotNull DBRShellCommand command, @NotNull Map<String, String> approvalContext)
    throws DBException {
        Boolean approved = UIUtils.syncExec(new RunnableWithResult<>() {
            @Override
            public Boolean runWithResult() {
                return new ConfirmShellCommandDialog(
                    UIUtils.getActiveWorkbenchShell(),
                    approvalContext,
                    command.getCommand()
                ).open() == Window.OK;
            }
        });
        if (Boolean.TRUE.equals(approved)) {
            addConfirmedCommand(command);
            return true;
        }
        return false;
    }

    private static class ConfirmShellCommandDialog extends BaseDialog {
        private static final int MINIMUM_WIDTH = 700;
        private static final int MAXIMUM_SCRIPT_LINES = 8;

        private final Map<String, String> approvalContext;
        private final String command;

        private ConfirmShellCommandDialog(
            @Nullable Shell parentShell,
            @NotNull Map<String, String> approvalContext,
            @NotNull String command
        ) {
            super(parentShell, CoreMessages.shell_cmd_manager_add_command_confirmation_label, null);
            this.approvalContext = approvalContext;
            this.command = command;
        }

        @NotNull
        @Override
        protected Composite createDialogArea(@NotNull Composite parent) {
            Composite area = super.createDialogArea(parent);
            ((GridLayout) area.getLayout()).numColumns = 2;

            Label warningImage = new Label(area, SWT.NONE);
            warningImage.setImage(DBeaverIcons.getImage(DBIcon.STATUS_WARNING));
            warningImage.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));

            Composite content = UIUtils.createComposite(area, 1);
            content.setLayoutData(new GridData(GridData.FILL_BOTH));

            Label description = UIUtils.createLabel(content, CoreMessages.shell_cmd_manager_add_command_confirmation_description);
            description.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            Composite context = UIUtils.createComposite(content, 2);
            context.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            for (Map.Entry<String, String> entry : approvalContext.entrySet()) {
                UIUtils.createControlLabel(context, entry.getKey());
                Label value = UIUtils.createLabel(context, entry.getValue());
                value.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            }

            UIUtils.createControlLabel(content, CoreMessages.shell_cmd_manager_add_command_script_label);
            Text script = new Text(content, SWT.BORDER | SWT.MULTI | SWT.READ_ONLY | SWT.WRAP | SWT.V_SCROLL);
            script.setFont(JFaceResources.getTextFont());
            script.setText(command);
            GridData scriptData = new GridData(GridData.FILL_BOTH);
            int lineCount = Math.min(MAXIMUM_SCRIPT_LINES, Math.max(2, script.getLineCount()));
            scriptData.heightHint = UIUtils.getFontHeight(script) * lineCount + 10;
            script.setLayoutData(scriptData);

            return area;
        }

        @Override
        protected void createButtonsForButtonBar(@NotNull Composite parent) {
            createButton(
                parent,
                IDialogConstants.OK_ID,
                CoreMessages.shell_cmd_manager_add_command_confirmation_button,
                false
            );
            createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, true);
        }
    }
}
