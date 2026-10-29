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
package org.jkiss.dbeaver.model.preferences;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.nio.file.Path;

public class ConfirmedShellCommandsStore {
    public static final String CONFIRMED_COMMANDS_FILE_NAME = "confirmed_shell_commands.json";

    private static ConfirmedShellCommandsStore instance;

    private ConfirmedActionsStore actions;

    private ConfirmedShellCommandsStore() {
    }

    @NotNull
    public static synchronized ConfirmedShellCommandsStore getInstance() {
        if (instance == null) {
            instance = new ConfirmedShellCommandsStore();
        }
        return instance;
    }

    public boolean contains(@NotNull String command) throws DBException {
        return getActions().contains(command);
    }

    public boolean add(@NotNull String command) throws DBException {
        return getActions().add(command);
    }

    @NotNull
    public Path prepareFile() throws DBException {
        return getActions().prepareFile();
    }

    @NotNull
    private synchronized ConfirmedActionsStore getActions() {
        if (actions == null) {
            actions = new ConfirmedActionsStore(DBWorkbench.getPlatform().getGlobalConfigurationFile(CONFIRMED_COMMANDS_FILE_NAME));
        }
        return actions;
    }
}
