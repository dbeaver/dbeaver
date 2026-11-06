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
package org.jkiss.dbeaver.model.datadam.sync.core;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.json.JSONUtils;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

final class DDConfigurationSyncStateStore {

    private static final Log log = Log.getLog(DDConfigurationSyncStateStore.class);

    private final Path file;

    DDConfigurationSyncStateStore(@NotNull Path file) {
        this.file = file.toAbsolutePath().normalize();
    }

    @Nullable
    State load() throws DBException {
        if (Files.notExists(file)) {
            return null;
        }
        try {
            State state = JSONUtils.GSON.fromJson(Files.readString(file), State.class);
            validate(state);
            return state;
        } catch (IOException | RuntimeException e) {
            throw new DBException("Error reading DataDam configuration sync state", e);
        }
    }

    void save(@NotNull State state) throws DBException {
        validate(state);
        Path temporary = null;
        try {
            Files.createDirectories(file.getParent());
            temporary = Files.createTempFile(file.getParent(), file.getFileName().toString(), ".tmp");
            Files.writeString(temporary, JSONUtils.PRETTY_GSON.toJson(state));
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new DBException("Error saving DataDam configuration sync state", e);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException e) {
                    log.debug("Error deleting temporary DataDam configuration sync state", e);
                }
            }
        }
    }

    private static void validate(@Nullable State state) throws DBException {
        if (state == null || state.configurationId() == null || state.configurationVersion() < 0 ||
            state.partVersion() < 1 || state.fingerprint() == null || state.fingerprint().isEmpty()) {
            throw new DBException("Invalid DataDam configuration sync state");
        }
    }

    record State(
        @NotNull UUID configurationId,
        long configurationVersion,
        long partVersion,
        @NotNull String fingerprint
    ) {
    }
}
