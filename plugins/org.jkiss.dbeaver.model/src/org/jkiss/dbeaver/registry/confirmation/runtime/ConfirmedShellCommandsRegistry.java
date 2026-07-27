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
package org.jkiss.dbeaver.registry.confirmation.runtime;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBConfigurationController;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.runtime.DBRShellCommand;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class ConfirmedShellCommandsRegistry {

    public static final String CONFIRMED_COMMANDS_FILE_NAME = "confirmed_shell_commands.json";

    private static ConfirmedShellCommandsRegistry instance;

    @NotNull
    public static synchronized ConfirmedShellCommandsRegistry getInstance() {
        if (instance == null) {
            instance = new ConfirmedShellCommandsRegistry();
        }
        return instance;
    }

    private ConfirmedShellCommandsRegistry() {
    }

    public boolean isConfirmedShellCommand(@NotNull DBRShellCommand command) throws DBException {
        String commandText = command.getCommand();
        return CommonUtils.isEmpty(commandText) || getConfirmedShellCommandsHolder().confirmedCommands().contains(command.getCommand());
    }

    public void addConfirmedShellCommand(@NotNull DBRShellCommand command) throws DBException {
        ConfirmedShellCommandsHolder confirmedShellCommandsHolder = getConfirmedShellCommandsHolder();
        String commandText = command.getCommand();
        if (CommonUtils.isNotEmpty(commandText) && confirmedShellCommandsHolder.addCommand(commandText)) {
            confirmedShellCommandsHolder.saveCommands();
        }
    }

    public void removeConfirmedShellCommand(@NotNull DBRShellCommand command) throws DBException {
        ConfirmedShellCommandsHolder confirmedShellCommandsHolder = getConfirmedShellCommandsHolder();
        String commandText = command.getCommand();
        if (CommonUtils.isNotEmpty(commandText) && confirmedShellCommandsHolder.removeCommand(commandText)) {
            confirmedShellCommandsHolder.saveCommands();
        }
    }

    @NotNull
    private ConfirmedShellCommandsHolder getConfirmedShellCommandsHolder() {
        return ConfirmedShellCommandsHolder.INSTANCE;
    }

    private static class ConfirmedShellCommandsHolder {


        public static final ConfirmedShellCommandsHolder INSTANCE = new ConfirmedShellCommandsHolder();
        Type targetClassType = new TypeToken<Set<String>>() {
        }.getType();

        public static final Gson GSON = JSONUtils.PRETTY_GSON;

        private Set<String> confirmedCommands;

        private ConfirmedShellCommandsHolder() {
        }

        @NotNull
        public synchronized Set<String> confirmedCommands() throws DBException {
            if (confirmedCommands == null) {
                String loaded = getConfigurationController()
                    .loadConfigurationFile(CONFIRMED_COMMANDS_FILE_NAME);
                confirmedCommands = Objects.requireNonNullElse(GSON.fromJson(loaded, targetClassType), new HashSet<>());
            }
            return confirmedCommands;
        }

        public boolean addCommand(@NotNull String command) throws DBException {
            return confirmedCommands().add(command);
        }

        public boolean removeCommand(@NotNull String command) throws DBException {
            return confirmedCommands().remove(command);
        }


        public void reset() {
            this.confirmedCommands = null;
        }

        @NotNull
        private DBConfigurationController getConfigurationController() {
            return DBWorkbench.getPlatform()
                .getConfigurationController();
        }

        private void saveCommands() throws DBException {
            getConfigurationController().saveConfigurationFile(CONFIRMED_COMMANDS_FILE_NAME, GSON.toJson(confirmedCommands));
            reset();
        }
    }

}
