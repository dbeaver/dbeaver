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
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class ConfirmedShellCommandsManager {

    private static final Log log = Log.getLog(ConfirmedShellCommandsManager.class);

    public static final String CONFIRMED_COMMANDS_FILE_NAME = "confirmed_shell_commands.json";

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

    public void validateCommandByUser(@NotNull DBRShellCommand command, @NotNull String approveByUserAdditionalContext) throws DBException {
        if (!command.isBlank()) {
            validateNotDistributed();
            boolean isApprovedByUser = confirmedCommands().contains(command.getCommand()) || askApproveForCommand(
                command,
                approveByUserAdditionalContext
            );
            if (!isApprovedByUser) {
                throw new DBException(NLS.bind(ModelMessages.shell_cmd_manager_add_command_error_message, approveByUserAdditionalContext));
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
        validateNotDistributed();
        return !command.isBlank() && addConfirmedShellCommand(command.getCommand());
    }

    public boolean removeConfirmedShellCommand(@NotNull DBRShellCommand command) throws DBException {
        validateNotDistributed();
        return !command.isBlank() && removeConfirmedShellCommand(command.getCommand());
    }

    private void validateNotDistributed() throws DBException {
        if (DBWorkbench.isDistributed()) {
            throw new DBException(ModelMessages.shell_cmd_manager_add_command_error_message_te_specific);
        }
    }

    private boolean askApproveForCommand(@NotNull DBRShellCommand command, @NotNull String approveByUserAdditionalContext)
    throws DBException {
        if (DBWorkbench.getPlatformUI().confirmAction(
            ModelMessages.shell_cmd_manager_add_command_confirmation_label,
            NLS.bind(ModelMessages.shell_cmd_manager_add_command_confirmation_text, approveByUserAdditionalContext, command.getCommand())
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
        var path = getConfigFilePath();
        if (Files.exists(path)) {
            try (var reader = Files.newBufferedReader(path)) {
                confirmedCommands = (Set<String>) JSONUtils.GSON.fromJson(reader, TypeToken.getParameterized(Set.class, String.class));
            } catch (Exception e) {
                log.error("Error loading product configuration state from " + path, e);
            }
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

    private void saveCommands() throws DBException {
        var path = getConfigFilePath();
        try {
            Files.createDirectories(path.getParent());
            try (var writer = Files.newBufferedWriter(path)) {
                JSONUtils.PRETTY_GSON.toJson(confirmedCommands, writer);
            }
        } catch (Exception e) {
            throw new DBException("Error saving confirmed commands", e);
        }
        log.debug("Saved confirmed commands to file '%s'".formatted(CONFIRMED_COMMANDS_FILE_NAME));
    }

    @NotNull
    private Path getConfigFilePath() {
        return DBWorkbench.getPlatform().getGlobalConfigurationFile(CONFIRMED_COMMANDS_FILE_NAME);
    }
}
