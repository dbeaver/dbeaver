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

import org.eclipse.osgi.util.NLS;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.preferences.ConfirmedShellCommandsStore;
import org.jkiss.dbeaver.model.runtime.DBRShellCommand;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.UIServiceShellCommands;

import java.util.List;

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
    public void validateByUser(@NotNull DBRShellCommand command, @NotNull List<String> approvalContext) throws DBException {
        if (command.isBlank()) {
            return;
        }

        String context = String.join(", ", approvalContext);
        boolean isApprovedByUser = confirmedCommandsStore.contains(command.getCommand())
            || askApproveForCommand(command, context);
        if (!isApprovedByUser) {
            throw new DBException(NLS.bind(
                CoreMessages.shell_cmd_manager_add_command_error_message,
                context
            ));
        }
    }

    private boolean askApproveForCommand(@NotNull DBRShellCommand command, @NotNull String approveByUserAdditionalContext)
    throws DBException {
        if (DBWorkbench.getPlatformUI().confirmAction(
            CoreMessages.shell_cmd_manager_add_command_confirmation_label,
            NLS.bind(
                CoreMessages.shell_cmd_manager_add_command_confirmation_text,
                approveByUserAdditionalContext,
                command.getCommand()
            ),
            CoreMessages.shell_cmd_manager_add_command_confirmation_button,
            false
        )) {
            addConfirmedCommand(command);
            return true;
        }
        return false;
    }
}
