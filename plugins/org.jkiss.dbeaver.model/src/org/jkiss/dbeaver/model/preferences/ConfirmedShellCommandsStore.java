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

import com.google.gson.reflect.TypeToken;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class ConfirmedShellCommandsStore {

    public static final String CONFIRMED_COMMANDS_FILE_NAME = "confirmed_shell_commands.json";

    private static final Log log = Log.getLog(ConfirmedShellCommandsStore.class);

    private static ConfirmedShellCommandsStore instance;

    @Nullable
    private Set<String> confirmedCommands;
    @Nullable
    private FileTime confirmedCommandsModificationTime;

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
        return confirmedCommands().contains(command);
    }

    public boolean add(@NotNull String command) throws DBException {
        synchronized (this) {
            confirmedCommands = loadConfirmedCommands();
            boolean result = confirmedCommands.add(command);
            log.debug("Tried to add confirmed command result: %s".formatted(result));
            saveCommands();
            return result;
        }
    }

    @NotNull
    public Path prepareFile() throws DBException {
        var path = getFilePath();
        try {
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                try (var writer = Files.newBufferedWriter(path)) {
                    JSONUtils.PRETTY_GSON.toJson(Set.of(), writer);
                }
            }
            synchronized (this) {
                confirmedCommands = loadConfirmedCommands();
            }
            return path;
        } catch (Exception e) {
            throw new DBException("Error preparing confirmed commands file: %s".formatted(path), e);
        }
    }

    @NotNull
    private Path getFilePath() {
        return DBWorkbench.getPlatform().getGlobalConfigurationFile(CONFIRMED_COMMANDS_FILE_NAME);
    }

    @NotNull
    private Set<String> confirmedCommands() throws DBException {
        if (confirmedCommands == null || isFileChanged()) {
            synchronized (this) {
                confirmedCommands = loadConfirmedCommands();
            }
        }
        return confirmedCommands;
    }

    @NotNull
    private Set<String> loadConfirmedCommands() {
        Set<String> commands = null;
        var path = getFilePath();
        if (Files.exists(path)) {
            try (var reader = Files.newBufferedReader(path)) {
                commands = JSONUtils.GSON.fromJson(reader, TypeToken.getParameterized(Set.class, String.class).getType());
                confirmedCommandsModificationTime = getFileModificationTime(path);
            } catch (Exception e) {
                log.error("Error loading confirmed shell commands from " + path, e);
                confirmedCommandsModificationTime = null;
            }
        } else {
            confirmedCommandsModificationTime = null;
        }
        return Objects.requireNonNullElse(commands, new HashSet<>());
    }

    private boolean isFileChanged() throws DBException {
        FileTime actualModificationTime = getFileModificationTime(getFilePath());
        return !Objects.equals(confirmedCommandsModificationTime, actualModificationTime);
    }

    private void saveCommands() throws DBException {
        var path = getFilePath();
        try {
            Files.createDirectories(path.getParent());
            try (var writer = Files.newBufferedWriter(path)) {
                JSONUtils.PRETTY_GSON.toJson(confirmedCommands, writer);
            }
            confirmedCommandsModificationTime = getFileModificationTime(path);
        } catch (Exception e) {
            throw new DBException("Error saving confirmed commands, file: %s".formatted(path), e);
        }
        log.debug("Saved confirmed commands to file '%s'".formatted(CONFIRMED_COMMANDS_FILE_NAME));
    }

    @Nullable
    private FileTime getFileModificationTime(@NotNull Path path) throws DBException {
        try {
            return Files.exists(path) ? Files.getLastModifiedTime(path) : null;
        } catch (Exception e) {
            throw new DBException("Error reading confirmed commands file modification time: %s".formatted(path), e);
        }
    }
}
