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

import com.google.gson.reflect.TypeToken;
import org.eclipse.osgi.util.NLS;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBConfigurationController;
import org.jkiss.dbeaver.model.app.DBAFeaturesConfig;
import org.jkiss.dbeaver.model.connection.DBPConnectionEventType;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class ConfirmedShellCommandsManager {

    private static final Log log = Log.getLog(ConfirmedShellCommandsManager.class);

    public static final String CONFIRMED_COMMANDS_FILE_NAME = "confirmed_shell_commands.json";
    public static final String SHELL_COMMANDS_ENABLED_DIST = "enableConnectionShellCmd";

    private static ConfirmedShellCommandsManager instance;

    @Nullable
    private Set<String> confirmedCommands;

    @NotNull
    public static synchronized ConfirmedShellCommandsManager getInstance() {
        if (instance == null) {
            instance = new ConfirmedShellCommandsManager();
        }
        return instance;
    }

    private ConfirmedShellCommandsManager() {
    }

    public void validateCommandByUser(@NotNull DBRShellCommand command, @NotNull DBPConnectionEventType eventType) throws DBException {
        if (!command.isBlank()) {
            if (!DBWorkbench.isDistributed()) {
                boolean isApprovedByUser = confirmedCommands().contains(command.getCommand()) || askApproveForCommand(command);
                if (!isApprovedByUser) {
                    throw new DBException(NLS.bind(ModelMessages.shell_cmd_manager_add_command_error_message, eventType.getTitle()));
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
        return !command.isBlank() && addConfirmedShellCommand(command.getCommand());
    }

    public boolean removeConfirmedShellCommand(@NotNull DBRShellCommand command) throws DBException {
        if (DBWorkbench.isDistributed()) {
            validateForDistributed();
            //always false, since in TE for now command security management is disabled, and commands are not checked to be approved first
            return false;
        }
        return !command.isBlank() && removeConfirmedShellCommand(command.getCommand());
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

    private boolean askApproveForCommand(@NotNull DBRShellCommand command) throws DBException {
        if (DBWorkbench.getPlatformUI().confirmAction(
            ModelMessages.shell_cmd_manager_add_command_confirmation_label,
            NLS.bind(ModelMessages.shell_cmd_manager_add_command_confirmation_text, command.getCommand())
        )) {
            addConfirmedShellCommand(command);
            return true;
        }
        return false;
    }

    @NotNull
    private Set<String> confirmedCommands() throws DBException {
        if (confirmedCommands == null) {
            synchronized (this) {
                confirmedCommands = loadConfirmedCommandsForRepo();
            }
        }
        return confirmedCommands;
    }

    @NotNull
    private Set<String> loadConfirmedCommandsForRepo() throws DBException {
        Set<String> confirmedCommands = null;
        String loaded = getConfigurationController().loadConfigurationFile(CONFIRMED_COMMANDS_FILE_NAME);
        if (loaded != null) {
            confirmedCommands = (Set<String>) JSONUtils.GSON.fromJson(
                loaded,
                TypeToken.getParameterized(Set.class, String.class)
            );
        }
        return Objects.requireNonNullElse(confirmedCommands, new HashSet<>());
    }

    private boolean addConfirmedShellCommand(@NotNull String command) throws DBException {
        synchronized (this) {
            confirmedCommands = loadConfirmedCommandsForRepo();
            boolean result = confirmedCommands.add(command);
            log.debug("Tried to add confirmed command result: %s".formatted(result));
            saveCommands();
            return result;
        }
    }

    private boolean removeConfirmedShellCommand(@NotNull String command) throws DBException {
        synchronized (this) {
            confirmedCommands = loadConfirmedCommandsForRepo();
            boolean result = confirmedCommands.remove(command);
            log.debug("Tried to remove confirmed command result: %s".formatted(result));
            saveCommands();
            return result;
        }
    }

    @NotNull
    private DBConfigurationController getConfigurationController() {
        return DBWorkbench.getPlatform().getConfigurationController();
    }

    private void saveCommands() throws DBException {
        getConfigurationController().saveConfigurationFile(CONFIRMED_COMMANDS_FILE_NAME, JSONUtils.PRETTY_GSON.toJson(confirmedCommands));
        log.debug("Saved confirmed commands to file '%s'".formatted(CONFIRMED_COMMANDS_FILE_NAME));
    }
}
