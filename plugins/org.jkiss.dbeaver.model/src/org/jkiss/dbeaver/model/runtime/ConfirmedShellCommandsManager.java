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
package org.jkiss.dbeaver.model.runtime;/*
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


import org.eclipse.osgi.util.NLS;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.connection.DBPConnectionEventType;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.registry.confirmation.runtime.ConfirmedShellCommandsRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;

public class ConfirmedShellCommandsManager {


    public void validateCommand(@NotNull DBRShellCommand command, @NotNull DBPConnectionEventType eventType) throws DBException {
        if (!command.isBlank()) {
            if (DBWorkbench.isDistributed()) {
                throw new DBException(ModelMessages.shell_cmd_manager_add_command_error_message_te_specific);
            } else if (!isApprovedCommand(command)) {
                throw new DBException(NLS.bind(ModelMessages.shell_cmd_manager_add_command_error_message, eventType.getTitle()));
            }
        }
    }

    public void addConfirmedShellCommand(@NotNull DBRShellCommand command) throws DBException {
        getConfirmedShellCommandsRegistry().addConfirmedShellCommand(command);
    }

    private boolean isApprovedCommand(@NotNull DBRShellCommand command) throws DBException {
        return getConfirmedShellCommandsRegistry().isConfirmedShellCommand(command)
            || askApproveForCommand(command);
    }

    private boolean askApproveForCommand(@NotNull DBRShellCommand command) throws DBException {
        if (DBWorkbench
            .getPlatformUI()
            .confirmAction(
                ModelMessages.shell_cmd_manager_add_command_confirmation_label,
                NLS.bind(ModelMessages.shell_cmd_manager_add_command_confirmation_text, command.getCommand())
            )
        ) {
            addConfirmedShellCommand(command);
            return true;
        }
        return false;
    }


    @NotNull
    private ConfirmedShellCommandsRegistry getConfirmedShellCommandsRegistry() {
        return ConfirmedShellCommandsRegistry.getInstance();
    }

}
