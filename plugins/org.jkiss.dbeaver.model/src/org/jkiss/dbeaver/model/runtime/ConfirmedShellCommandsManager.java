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
package org.jkiss.dbeaver.model.runtime;

import org.eclipse.osgi.util.NLS;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.app.DBAFeaturesConfig;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.preferences.ConfirmedShellCommandsStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;

public class ConfirmedShellCommandsManager {

    public static final String SHELL_COMMANDS_ENABLED_DIST = "enableConnectionShellCmd";

    private static ConfirmedShellCommandsManager instance;

    private final ConfirmedShellCommandsStore confirmedCommandsStore = ConfirmedShellCommandsStore.getInstance();

    @NotNull
    public static synchronized ConfirmedShellCommandsManager getInstance() {
        if (instance == null) {
            instance = new ConfirmedShellCommandsManager();
        }
        return instance;
    }

    private ConfirmedShellCommandsManager() {
    }

    public void validateCommandByUser(@NotNull DBRShellCommand command, @NotNull String approveByUserAdditionalContext) throws DBException {
        if (!command.isBlank()) {
            if (!DBWorkbench.isDistributed()) {
                boolean isApprovedByUser = confirmedCommandsStore.contains(command.getCommand())
                    || askApproveForCommand(command, approveByUserAdditionalContext);
                if (!isApprovedByUser) {
                    throw new DBException(NLS.bind(
                        ModelMessages.shell_cmd_manager_add_command_error_message,
                        approveByUserAdditionalContext
                    ));
                }
            } else {
                validateForDistributed();
            }
        }
    }

    /**
     * Saves command in confirmed by user commands
     *
     * @param command to be save
     * @return true if command is successfully added. false if not, for example command was already present in confirmed list
     * @throws DBException in case smth bad happens while adding
     */
    public boolean addConfirmedShellCommand(@NotNull DBRShellCommand command) throws DBException {
        if (DBWorkbench.isDistributed()) {
            validateForDistributed();
            //always false, since in TE for now command security management is disabled, and commands are not checked to be approved first
            return false;
        }
        return !command.isBlank() && confirmedCommandsStore.add(command.getCommand());
    }

    private void validateForDistributed() throws DBException {
        if (!isFeatureEnabledInDistributed()) {
            throw new DBException(ModelMessages.shell_cmd_manager_add_command_error_message_te_specific);
        }
    }

    public boolean isFeatureEnabledInDistributed() {
        var featureChecker = DBWorkbench.getService(DBAFeaturesConfig.class);
        return featureChecker != null && featureChecker.isServiceEnabled(SHELL_COMMANDS_ENABLED_DIST);
    }

    private boolean askApproveForCommand(@NotNull DBRShellCommand command, @NotNull String approveByUserAdditionalContext)
    throws DBException {
        if (DBWorkbench.getPlatformUI().confirmAction(
            ModelMessages.shell_cmd_manager_add_command_confirmation_label,
            NLS.bind(
                ModelMessages.shell_cmd_manager_add_command_confirmation_text, approveByUserAdditionalContext, command.getCommand()
            ),
            ModelMessages.shell_cmd_manager_add_command_confirmation_button,
            false
        )) {
            addConfirmedShellCommand(command);
            return true;
        }
        return false;
    }

}
